/*
 * Copyright 2022 CozyDev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pink.cozydev.protosearch

import cats.data.NonEmptyList
import pink.cozydev.lucille.Query
import pink.cozydev.lucille.Query.{Term, Field, Prefix, Group, Or}

object LastTermRewrite {

  def termToPrefix(q: Query): Query =
    q match {
      case Term(t) =>
        Group(NonEmptyList.one(Or(NonEmptyList.of(Term(t), Prefix(t)))))
      case Field(fn, Term(t)) =>
        Group(
          NonEmptyList.one(Or(NonEmptyList.of(Field(fn, Term(t)), Field(fn, Prefix(t)))))
        )
      case _ => q
    }

  def lastTermPrefix(qs: NonEmptyList[Query]): NonEmptyList[Query] =
    rewrite(qs, termToPrefix)

  def rewrite(qs: NonEmptyList[Query], func: Query => Query): NonEmptyList[Query] =
    if (qs.size == 0) qs
    else if (qs.size == 1) NonEmptyList.one(func(qs.head))
    else {
      val newT = qs.tail.init :+ func(qs.last)
      NonEmptyList(qs.head, newT)
    }
}
