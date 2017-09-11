package ubodin

import chandu0101.scalajs.react.components._


import scalacss.Defaults._
import scalacss.ScalaCssReact._
import scalacss.internal.mutable.GlobalRegistry

object ClientAppCSS {
  def load(): Unit = {
    GlobalRegistry.register(
      ReactTable.DefaultStyle,
      ReactListView.DefaultStyle,
      ReactSearchBox.DefaultStyle,
      Pager.DefaultStyle,
      ReactDraggable.Style,
      ODInTable.DefaultStyle,
      UBOdinMobileTearSheet.Style
    )

    GlobalRegistry.addToDocumentOnRegistration()
  }
}
