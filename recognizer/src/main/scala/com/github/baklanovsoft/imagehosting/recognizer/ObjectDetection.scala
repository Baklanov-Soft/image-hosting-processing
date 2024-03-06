package com.github.baklanovsoft.imagehosting.recognizer

import ai.djl.Application
import ai.djl.engine.Engine
import ai.djl.inference.Predictor
import ai.djl.modality.cv.Image
import ai.djl.modality.cv.output.DetectedObjects
import ai.djl.repository.zoo.{Criteria, ZooModel}
import cats.effect.kernel.{Resource, Sync}
import cats.implicits._
import com.github.baklanovsoft.imagehosting.s3.MinioClient
import com.github.baklanovsoft.imagehosting.{BucketId, Category, ImageId, Score}
import org.typelevel.log4cats.LoggerFactory

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}
import scala.jdk.CollectionConverters._

trait ObjectDetection[F[_]] {

  def detect(image: Image, bucketId: BucketId, imageId: ImageId): F[Map[Category, Score]]

}

object ObjectDetection {

  private object Engines {

    // mx net has a lot of included models so it's better to use in detection
    object MxNet {
      val name = "MXNet"

      object Models {
        val darknet53 = "darknet53"
        val mobileNet = "mobilenet1.0"
        val resnet50  = "resnet50"
        val vgg16     = "vgg16"
      }
    }

    object PyTorch {
      val name = "MXNet"

      object Models {
        val resnet50 = "resnet50"
      }
    }
  }

  private def acquireModelPredictor[F[_]: Sync]
      : Resource[F, (ZooModel[Image, DetectedObjects], Predictor[Image, DetectedObjects])] =
    Resource.make {
      val useEngine = Engines.PyTorch

      for {
        criteria <- Sync[F].delay(
                      Criteria.builder
                        .optApplication(Application.CV.OBJECT_DETECTION)
                        .setTypes(classOf[Image], classOf[DetectedObjects])
                        .optEngine(Engine.getEngine(useEngine.name).getEngineName)
                        .optFilter("backbone", useEngine.Models.resnet50)
                        .build
                    )

        model <- Sync[F].delay(criteria.loadModel())

        predictor <- Sync[F].delay(model.newPredictor())
      } yield (model, predictor)

    } { case (model, predictor) => Sync[F].delay(predictor.close()) >> Sync[F].delay(model.close()) }

  private def predict[F[_]: Sync](predictor: Predictor[Image, DetectedObjects], image: Image) =
    Sync[F].delay(predictor.predict(image))

  private def toCategories(detectedObjects: DetectedObjects): Map[Category, Score] =
    detectedObjects.getClassNames.asScala
      .zip(detectedObjects.getProbabilities.asScala)
      .groupBy(_._1) /* this is done for cases where multiple objects of the same category found - we'll take only the
       * biggest score */
      .map(t => t._1 -> t._2.map(_._2).max)
      .map { case (category, score) => (Category(category), Score(score)) }

  def production[F[_]: Sync: LoggerFactory]: Resource[F, ObjectDetection[F]] = for {
    logger         <- Resource.eval(LoggerFactory[F].create)
    (_, predictor) <- acquireModelPredictor[F]
  } yield new ObjectDetection[F] {
    override def detect(image: Image, bucketId: BucketId, imageId: ImageId): F[Map[Category, Score]] =
      for {
        detected  <- predict(predictor, image)
        categories = toCategories(detected)
        _         <- logger.info(s"Detection result $bucketId:$imageId: $detected")
      } yield categories
  }

  def debug[F[_]: Sync: LoggerFactory](minioClient: MinioClient[F]): Resource[F, ObjectDetection[F]] =
    for {
      logger         <- Resource.eval(LoggerFactory[F].create)
      (_, predictor) <- acquireModelPredictor[F]
    } yield new ObjectDetection[F] {

      /** Will create image with detected objects in rectangles
        */
      private def saveDebugImage(
          image: Image,
          detectedObjects: DetectedObjects,
          bucketId: BucketId,
          imageId: ImageId
      ): F[Unit] = for {
        // don't touch initial image since it could be used in other places and it is mutable
        cloneImage <- Sync[F].delay(image.duplicate())
        // mutation on Image
        _          <- Sync[F].delay(cloneImage.drawBoundingBoxes(detectedObjects))

        _ <- Sync[F].bracket {
               Sync[F].delay {
                 // saving to output stream but piping to input stream
                 val out             = new ByteArrayOutputStream()
                 cloneImage.save(out, "png")
                 val in: InputStream = new ByteArrayInputStream(out.toByteArray)
                 (out, in)
               }
             } { case (_, in) =>
               minioClient.putObject(
                 bucketId = bucketId,
                 objectName = imageId.value.toString,
                 stream = in,
                 contentType = "image/png",
                 folder = Some("debug")
               )
             } { case (out, in) =>
               Sync[F].delay {
                 in.close()
                 out.close()
               }
             }
        _ <- logger.info(s"Saved debug for image $bucketId:$imageId")
      } yield ()

      override def detect(image: Image, bucketId: BucketId, imageId: ImageId): F[Map[Category, Score]] =
        for {
          detected  <- predict(predictor, image)
          categories = toCategories(detected)
          _         <- logger.info(s"Detection result $bucketId:$imageId: $detected")
          _         <- Sync[F].whenA(categories.nonEmpty)(saveDebugImage(image, detected, bucketId, imageId))
        } yield categories
    }

}
