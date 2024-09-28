package com.github.baklanovsoft.imagehosting

/** Information for s3 on how to extract/put image
  */
final case class ImageMeta(
    bucket: BucketId,
    prefix: Prefix,
    name: ImageId
) {
  val path = s"$prefix/$name"
}
