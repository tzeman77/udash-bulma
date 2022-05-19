/*
 * Copyright 2021 Tomas Zeman <tomas@functionals.cz>
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
package bulma

import bulma.Bulma._
import io.udash._
import io.udash.bindings.inputs.InputBinding
import io.udash.css.CssView._
import org.scalajs.dom.ext.KeyCode
import org.scalajs.dom.html.{Div, Input}
import org.scalajs.dom.{Element, Event, KeyboardEvent}
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._

/**
 * Autocomplete widget based on text input + dropdown menu.
 * @param selected (To be) selected object.
 * @param blank Empty object.
 * @tparam T Object type.
 */
abstract class Autocomplete[T](val selected: Property[T])(
  implicit blank: Blank[T]) {

  protected val busy: Property[Boolean] = Property(false)
  protected val q: Property[String] = Property("")
  protected val list: SeqProperty[T] = SeqProperty.blank
  protected val active: Property[T] = Property.blank[T]

  protected def keyHandler(ev: KeyboardEvent): Boolean = ev.keyCode match {
    case KeyCode.Enter =>
      list.clear()
      selected.set(active.get)
      true
    case KeyCode.Up =>
      val l = list.get
      active.set((l :+ blank.value) zip((l takeRight 1) ++ l)
        find(_._1 == active.get) map(_._2) getOrElse blank.value)
      true
    case KeyCode.Down =>
      val l = list.get
      active.set((blank.value +: l) zip(l ++ (l take 1))
        find(_._1 == active.get) map(_._2) getOrElse blank.value)
      true
    case _ => false
  }

  protected def clickHandler(cur: T)(ev: Event): Unit = {
    ev.preventDefault()
    list.clear()
    selected.set(cur)
  }

  protected def menuItem(v: T): Modifier = v.toString

  protected def renderMenuItem(act: T, cur: T): TypedTag[Element] =
    a(dropdownItem, isActive.styleIf(cur == act), href:="#", menuItem(cur),
      onclick:+=clickHandler(cur))

  protected def placeHolder: String = ""

  protected def renderMenu: TypedTag[Div] = div(dropdownMenu, width:="100%",
    showIf(list transform(_.nonEmpty))(
      div(dropdownContent, repeatWithNested(list)((cur, nested) => span(
        nested(produce(active)(act => renderMenuItem(act, cur.get).render))
      ).render)).render))

  /**
   * Modifiers common to control-field-div and text-input.
   * @return Modifiers
   */
  protected def commonModifiers: Seq[Modifier] = Seq()

  protected def inputModifiers: Seq[Modifier] = Seq(autofocus)

  protected def fieldModifiers: Seq[Modifier] = Seq()

  protected def renderInput: InputBinding[Input] =
    TextInput(q)(input_, commonModifiers, inputModifiers,
      placeholder:=placeHolder, onkeydown:+=keyHandler)

  def tpl: TypedTag[Div] =
    div(field, dropdown, isActive.styleIf(list transform(_.nonEmpty)),
      fieldModifiers,
      div(control, isLoading.styleIf(busy), commonModifiers,
        renderInput, renderMenu))

}
