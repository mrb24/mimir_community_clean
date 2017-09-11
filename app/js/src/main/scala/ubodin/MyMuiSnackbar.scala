package ubodin


import chandu0101.macros.tojs.JSMacro
import japgolly.scalajs.react._
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.`|`
import chandu0101.scalajs.react.components.materialui.Mui
import chandu0101.scalajs.react.components.CssProperties
/**
 * This file is generated - submit issues instead of PR against it
 */
    
case class MyMuiSnackbar(
  key:              js.UndefOr[String]                      = js.undefined,
  ref:              js.UndefOr[MyMuiSnackbarM => Unit]        = js.undefined,
  /* The label for the action on the snackbar. */
  action:           js.UndefOr[ReactNode]                   = js.undefined,
  /* The number of milliseconds to wait before automatically dismissing.
     If no value is specified the snackbar will dismiss normally.
     If a value is provided the snackbar can still be dismissed normally.
     If a snackbar is dismissed before the timer expires, the timer will be cleared. */
  autoHideDuration: js.UndefOr[Int]                         = js.undefined,
  /* Override the inline-styles of the body element. */
  bodyStyle:        js.UndefOr[CssProperties]               = js.undefined,
  /* The css class name of the root element. */
  className:        js.UndefOr[String]                      = js.undefined,
  /* Override the inline-styles of the content element. */
  contentStyle:     js.UndefOr[CssProperties]               = js.undefined,
  /* The message to be displayed.
     (Note: If the message is an element or array, and the `Snackbar` may re-render while it is still open,
     ensure that the same object remains as the `message` property if you want to avoid the `Snackbar` hiding and
     showing again) */
  message:          ReactNode,
  /* Fired when the action button is touchtapped.
     @param {object} event Action button event. */
  onActionTouchTap: js.UndefOr[ReactTouchEvent => Callback] = js.undefined,
  /* Fired when the `Snackbar` is requested to be closed by a click outside the `Snackbar`, or after the
     `autoHideDuration` timer expires.
     Typically `onRequestClose` is used to set state in the parent component, which is used to control the `Snackbar`
     `open` prop.
     The `reason` parameter can optionally be used to control the response to `onRequestClose`,
     for example ignoring `clickaway`.
     @param {string} reason Can be:`"timeout"` (`autoHideDuration` expired) or: `"clickaway"` */
  onRequestClose:   js.UndefOr[String => Callback]          = js.undefined,
  /* Controls whether the `Snackbar` is opened or not. */
  open:             Boolean,
  /* Override the inline-styles of the root element. */
  style:            js.UndefOr[CssProperties]               = js.undefined){

  def apply(children: ReactNode*) = {
    val props = JSMacro[MyMuiSnackbar](this)
    val f = React.asInstanceOf[js.Dynamic].createFactory(Mui.Snackbar)
    if (children.isEmpty)
      f(props).asInstanceOf[ReactComponentU_]
    else if (children.size == 1)
      f(props, children.head).asInstanceOf[ReactComponentU_]
    else
      f(props, children.toJsArray).asInstanceOf[ReactComponentU_]
  }
}
        


@js.native
class MyMuiSnackbarM extends js.Object {
  def setAutoHideTimer(): js.Dynamic = js.native

  def setTransitionTimer(): js.Dynamic = js.native
}
