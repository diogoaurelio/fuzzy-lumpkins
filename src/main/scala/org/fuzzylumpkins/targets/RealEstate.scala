package org.fuzzylumpkins.targets

object RealEstate {
  sealed trait Company
  case object Century21 extends Company
  case object Era extends Company
}
