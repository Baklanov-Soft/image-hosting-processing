package com.github.baklanovsoft.imagehosting.error

import scala.util.control.NoStackTrace

trait AppError      extends NoStackTrace
trait DecodingError extends AppError
