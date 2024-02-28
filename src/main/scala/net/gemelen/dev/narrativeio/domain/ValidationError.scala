package net.gemelen.dev.narrativeio.domain

sealed trait ValidationError {
  def errorMessage: String
}

case object TimestampIsOutOfRange extends ValidationError {
  override def errorMessage: String = "Provided timestamp is not within the realistic time range"
}

case object UserIdIsOutOfRange extends ValidationError {
  override def errorMessage: String = "User Id doesn't conform expectations"
}

case object EventIsUnknown extends ValidationError {
  override def errorMessage: String = "Event is of an unknown type"
}
