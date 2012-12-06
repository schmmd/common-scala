package edu.washington.cs.knowitall.collection.immutable

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import org.specs2._
import org.scalacheck._

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

@RunWith(classOf[JUnitRunner])
object IntervalSpecTest extends Specification with ScalaCheck {
  implicit def arbInterval: Arbitrary[Interval] = Arbitrary {
    for {
      a <- Gen.choose(0, 20)
      b <- Gen.choose(0, 20) suchThat (_ >= a)
    } yield Interval.open(a, b)
  }

  "intersection is smaller" ! check { (a: Interval, b: Interval) => (a intersect b).size <= math.min(a.size, b.size) }
  "intersection is contained by both" ! check { (a: Interval, b: Interval) => (a intersect b).forall(x => (a contains x) && (b contains x)) }

  "union is larger" ! check { (a: Interval, b: Interval) => !(a intersects b) || (a union b).size >= math.min(a.size, b.size) }
  "union is contained by one" ! check { (a: Interval, b: Interval) => !(a intersects b) || (a union b).forall(x => (a contains x) || (b contains x)) }

  "intervals" should {
    "border each other" in {
      (Interval.open(0, 4) borders Interval.open(4, 8)) must beTrue
      (Interval.open(4, 8) borders Interval.open(0, 4)) must beTrue
      (Interval.open(0, 3) borders Interval.open(4, 8)) must beFalse
      (Interval.open(4, 8) borders Interval.open(0, 3)) must beFalse
      (Interval.empty borders Interval.open(4, 8)) must beFalse
    }

    "union properly" in {
      (Interval.open(0, 4) union Interval.open(4, 8)) must_== (Interval.open(0, 8))
      (Interval.open(0, 4) union Interval.open(6, 8)) must throwA[IllegalArgumentException]
    }

    "intersect properly" in {
      (Interval.open(0, 4) intersect Interval.open(4, 8)) must_== (Interval.empty)
      (Interval.open(0, 4) intersect Interval.open(6, 8)) must_== (Interval.empty)
      (Interval.open(0, 4) intersect Interval.open(2, 6)) must_== (Interval.open(2, 4))
    }

    "contain properly" in {
      Interval.open(2, 3) contains 0 must_== false
      Interval.open(2, 3) contains 1 must_== false
      Interval.open(2, 3) contains 2 must_== true
      Interval.open(2, 3) contains 3 must_== false
    }
  }

  "the correct left interval is determined" in {
    (Interval.open(0, 4) left Interval.open(4, 8)) must_== (Interval.open(0, 4))
    (Interval.open(0, 4) left Interval.open(2, 6)) must_== (Interval.open(0, 4))
    (Interval.open(4, 8) left Interval.open(0, 4)) must_== (Interval.open(0, 4))
    (Interval.open(2, 6) left Interval.open(0, 4)) must_== (Interval.open(0, 4))
  }

  "the correct right interval is determined" in {
    (Interval.open(0, 4) right Interval.open(4, 8)) must_== (Interval.open(4, 8))
    (Interval.open(0, 4) right Interval.open(2, 6)) must_== (Interval.open(2, 6))
    (Interval.open(4, 8) right Interval.open(0, 4)) must_== (Interval.open(4, 8))
    (Interval.open(2, 6) right Interval.open(0, 4)) must_== (Interval.open(2, 6))
  }

  "overlapping intervals have distance 0" in {
    (Interval.open(0, 4) distance Interval.open(2, 6)) must_== (0)
    (Interval.open(2, 6) distance Interval.open(0, 3)) must_== (0)
  }

  "intervals have the correct distance" in {
    (Interval.open(0, 2) distance Interval.open(2, 5)) must_== (1)
    (Interval.open(0, 2) distance Interval.open(3, 5)) must_== (2)
    (Interval.open(0, 2) distance Interval.open(4, 6)) must_== (3)
  }

  "adjacent intervals have the empty set between them" in {
    Interval.between(Interval.open(0, 2), Interval.open(2, 3)) must_== (Interval.empty)
  }

  "between works properly" in {
    Interval.between(Interval.open(0, 2), Interval.open(3, 10)) must_== (Interval.open(2, 3))
    Interval.between(Interval.open(0, 2), Interval.open(6, 10)) must_== (Interval.open(2, 6))
  }

  "empty works properly" in {
    (Interval.empty union Interval.open(2, 4)) must_== (Interval.open(2, 4))
    (Interval.empty intersect Interval.open(2, 4)) must_== (Interval.empty)

    (Interval.empty left Interval.open(2, 4)) must_== (Interval.open(2, 4))
    (Interval.open(2, 4) left Interval.empty) must_== (Interval.open(2, 4))
    (Interval.empty right Interval.open(2, 4)) must_== (Interval.open(2, 4))
    (Interval.open(2, 4) right Interval.empty) must_== (Interval.open(2, 4))

    (Interval.open(2, 4) subset Interval.empty) must beFalse
    (Interval.empty subset Interval.open(2, 4)) must beTrue

    (Interval.open(2, 4) superset Interval.empty) must beTrue
    (Interval.empty superset Interval.open(2, 4)) must beFalse
  }

  "serialization works properly" in {

    def testSerDeser(interval: Interval): Unit = {

      val bytesOutStream = new ByteArrayOutputStream()
      val objOutStream = new ObjectOutputStream(bytesOutStream)
      objOutStream.writeObject(interval)
      val outputBytes = bytesOutStream.toByteArray
      objOutStream.close()
      bytesOutStream.close()

      val bytesInStream = new ByteArrayInputStream(outputBytes)
      val objInStream = new ObjectInputStream(bytesInStream)
      val deserializedInterval = objInStream.readObject.asInstanceOf[Interval]
      objInStream.close()
      bytesInStream.close()

      deserializedInterval must_== interval
    }

    testSerDeser(Interval.empty)
    testSerDeser(Interval.closed(5, 10))
    testSerDeser(Interval.open(100, 200))
    testSerDeser(Interval.singleton(1234))
  }
}
