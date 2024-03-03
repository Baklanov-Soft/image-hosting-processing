package com.github.baklanovsoft.imagehosting.recognizer

import ai.djl.Application
import ai.djl.modality.Classifications
import ai.djl.modality.cv.Image
import ai.djl.modality.cv.transform.{CenterCrop, Normalize, Resize, ToTensor}
import ai.djl.modality.cv.translator.ImageClassificationTranslator
import ai.djl.repository.zoo.Criteria
import ai.djl.translate.Translator
import cats.effect.kernel.{Resource, Sync}
import cats.implicits._
import com.github.baklanovsoft.imagehosting.{BucketId, Category, ImageId, Score}
import org.typelevel.log4cats.LoggerFactory

import java.nio.file.Paths
import scala.jdk.CollectionConverters._

trait NsfwDetection[F[_]] {

  /** Will return nsfw category with score if nsfw detected
    */
  def detect(image: Image, bucketId: BucketId, imageId: ImageId): F[Option[(Category, Score)]]
}

object NsfwDetection {

  private def buildTranslator[F[_]: Sync](synsetPath: String): F[Translator[Image, Classifications]] = Sync[F].delay {
    // copypasted from here https://github.com/deepjavalibrary/djl/issues/1419
    ImageClassificationTranslator
      .builder()
      .optSynsetArtifactName(synsetPath)
      .addTransform(new Resize(256))
      // from the model description it was trained on 224x224 images so looks like it fits
      .addTransform(new CenterCrop(224, 224))
      .addTransform(new ToTensor())
      .addTransform(
        new Normalize(
          Array(
            0.485f,
            0.456f,
            0.406f
          ),
          Array(
            0.229f,
            0.224f,
            0.225f
          )
        )
      )
      .optApplySoftmax(true)
      .build()
  }

  private def acquireModelPredictor[F[_]: Sync](modelPath: String, synsetPath: String) =
    Resource.make {
      for {
        translator <- buildTranslator(synsetPath)
        criteria   <- Sync[F].delay {
                        Criteria
                          .builder()
                          .setTypes(classOf[Image], classOf[Classifications])
                          .optApplication(
                            Application.CV.IMAGE_CLASSIFICATION
                          )
                          .optTranslator(
                            translator
                          )
                          .optEngine("PyTorch")
                          .optModelPath(Paths.get(modelPath))
                          .build()
                      }

        model     <- Sync[F].delay(criteria.loadModel())
        predictor <- Sync[F].delay(model.newPredictor())
      } yield (model, predictor)
    } { case (model, predictor) => Sync[F].delay(predictor.close()) >> Sync[F].delay(model.close()) }

  def of[F[_]: Sync: LoggerFactory](modelPath: String, synsetPath: String): Resource[F, NsfwDetection[F]] =
    for {
      logger         <- Resource.eval(LoggerFactory[F].create)
      (_, predictor) <- acquireModelPredictor[F](modelPath, synsetPath)
    } yield new NsfwDetection[F] {

      override def detect(image: Image, bucketId: BucketId, imageId: ImageId): F[Option[(Category, Score)]] =
        for {
          detected <- Sync[F].delay(predictor.predict(image))
          _        <- logger.info(s"NSFW detection result for image $bucketId:$imageId: $detected")
        } yield detected.getClassNames.asScala
          .zip(detected.getProbabilities.asScala)
          .toMap
          .get("nsfw") // should be the same as in synset
          .filter(_ >= 0.7d)
          .map(java => Double2double(java))
          .map(score => (Category("nsfw"), Score(score)))

    }
}
