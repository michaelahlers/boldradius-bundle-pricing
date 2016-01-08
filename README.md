# Bundle Pricing for [BoldRadius Solutions](http://boldradius.com)

## Abstract

This exercise is a common problem in e­commerce and brick-and-mortar retail systems.

A customer shops from some form of catalog, selecting items and quantities they wish to purchase. When they are ready, they “check out”, that is, complete their purchase, at which point they are given a total amount of money they owe for the purchase of a specific set of items.

In the bounds of this problem, certain groups of items can be taken together as a “bundle” with a different price. For example, if I buy a single apple in isolation it costs $1.99, if I buy two apples it’s $2.15. More complex combinations are possible. For example, a loaf of bread “A” purchased with two sticks of margarine “B” and the second stick of margarine is free.

The same item may appear in more than one bundle (_i.e._, any one “cart” of items might be able to be combined in more than one way).

## Development

### Setup

Apart from requiring Oracle's Java SE Development Kit 8 (JDK 8), this project is “batteries included.” Simply start [Typesafe Activator](http://typesafe.com/activator) from the project's root to get started (installing [SBT](http://scala-sbt.org/0.13/tutorial/Setup.html) is optional). It's recommended to use one of the following resources to obtain Java.

- [Web Upd8 PPA](http://webupd8.org/2012/09/install-oracle-java-8-in-ubuntu-via-ppa.html)
  - Best packages for Debian-based distributions (_e.g._, Debian, Ubuntu, Mint)
- [Oracle Java SE Development Kit 8 Downloads](http://oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
  - Packages for RPM-based distributions (_e.g._, RHEL, Fedora, CentOS).
  - Installer packages for OS X.
  - Installer packages for Windows.

### Testing

From an SBT shell, the unit test suite is run with:

```
test
```

Specific tests with:

```
test-only boldradius.catalog.ItemSpec
```

Test code coverage reports may be generated with:

```
;coverage;clean;test;coverageReport
```

(Visit `target/scala-2.11/scoverage-report/index.html` in your browser to see results.)

## Resources

### [Play JSON](http://playframework.com/documentation/2.4.x/ScalaJson)

JSON representations of local models are convenient for both API consumers and developers. Typesafe's library—from the Play Framework—is a convenient and powerful tool for these purposes.

### [Squants](http://squants.com)

This project deals with costs, and the Squants framework—which deals with quantities and units of measure—provides an outstanding, full-featured API for money.
