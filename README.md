# alumbra.generators

A collection of [test.check][tc] generators for [GraphQL][ql] queries.

[![Build Status](https://travis-ci.org/alumbra/alumbra.generators.svg?branch=master)](https://travis-ci.org/alumbra/alumbra.generators)
[![Clojars Artifact](https://img.shields.io/clojars/v/alumbra/alumbra.generators.svg)](https://clojars.org/alumbra)

This library can mainly be used to test GraphQL parsers (since the generated
queries might not be semantically correct).

[tc]: https://github.com/clojure/test.check
[ql]: http://graphql.org/

## Usage

__Document Generator__

This generates a GraphQL document as described in the [GraphQL
specification][ql-spec].

```clojure
(require '[alumbra.generators.document :refer [-document]])

(clojure.test.check.generators/sample -document 1)
;; => ("mutation X($h: [T]! = 0.8e-57753886, $Q: [K]! = 0.1693) { ...")
```

[ql-spec]: https://facebook.github.io/graphql/

__Schema Generator__

This generates a schema for the GraphQL type system.

```clojure
(require '[alumbra.generators.schema :refer [-schema]])

(clojure.test.check.generators/sample -schema 1)
;; => ("schema {query: O, mutation: B}\ninterface G {D(c: [I]): [O]}\nenum F {O}")
```

## License

```
MIT License

Copyright (c) 2016 Yannick Scherer

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
