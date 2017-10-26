package ubodin

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global => g}
import scala.scalajs.js.JSApp

object Client extends JSApp {

  val deviceFingerprint = fingerprint.Fingerprint.get();
  
  override def main(): Unit = {
    // remove waiting page stuff
    if (!js.isUndefined(g.loadingElement)) {
      g.document.body.removeChild(g.loadingElement)
      g.loadingElement = js.undefined
      dom.document.body.className.replace("pg-loading", "")
      dom.document.body.className += " pg-loaded"
    }
    ClientAppCSS.load()
    UBOdinAppRouter.router().render(dom.document.getElementById("container"))
  }
}