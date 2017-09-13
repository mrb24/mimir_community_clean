package ubodin


import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.collection.immutable
import scala.scalajs.js.Date
import scalacss.Defaults._
import scalacss.ScalaCssReact._
import chandu0101.scalajs.react.components._


object ODInTable {
  type Model = CleaningJobDataRow//Map[String, Any]

  /**
   *  ._1: String = column name
   *  ._2: Option[Any => ReactElement] = custom cell
   *  ._3: Option[(Model,Model) => Boolean] = sorting function
   *  ._4: Option[Double] = column width interms of flex property
   */
  type Config = (String, Option[Any => ReactElement], Option[(Model, Model) => Boolean],Option[Double])

  val ASC: String = "asc"
  val DSC: String = "dsc"

  class Style extends StyleSheet.Inline {

    import dsl._

    val reactTableContainer = style(display.flex,
      flexDirection.column)

    val table = style(display.flex,
      flexDirection.column,
      boxShadow := "0 1px 3px 0 rgba(0, 0, 0, 0.12), 0 1px 2px 0 rgba(0, 0, 0, 0.24)",
      media.maxWidth(740 px)(
        boxShadow := "none"
       )
      )

    val tableRow = style(display.flex,
      padding :=! "0.8rem",
      &.hover(
        backgroundColor :=! "rgba(244, 244, 244, 0.77)"
      ),
      media.maxWidth(740 px)(
        display.flex,
        flexDirection.column,
        textAlign.center,
        boxShadow := "0 1px 3px grey",
        margin(5 px)
      ),
      unsafeChild("div")(
        flex := "1"
      )
    )
    
    val nonDetTableRowStyle = style(display.flex,
          padding :=! "0.8rem",
          backgroundColor :=! "rgba(232, 232, 232, 0.77)",
          &.hover(
            backgroundColor :=! "rgba(244, 244, 244, 0.77)"
          ),
          media.maxWidth(740 px)(
            display.flex,
            flexDirection.column,
            textAlign.center,
            boxShadow := "0 1px 3px grey",
            margin(5 px)
          ),
          unsafeChild("div")(
            flex := "1"
          )
        )

    val tableHeader = style(fontWeight.bold,
      borderBottom :=! "1px solid #e0e0e0",
      tableRow)

    val settingsBar = style(display.flex,
      margin :=! "15px 0",
      justifyContent.spaceBetween
    )

    val sortIcon = styleF.bool(ascending => styleS(
      &.after(
        fontSize(9 px),
        marginLeft(5 px),
        if (ascending) content := "'\\25B2'"
        else content := "'\\25BC'"
      )
    ))
    
    val idToStyle = Map("NonDetRowStyle" -> nonDetTableRowStyle)
    
  }

  object DefaultStyle extends Style

  case class State(
    filterText: String,
    offset: Int,
    rowsPerPage: Int,
    filteredModels: Vector[Model],
    sortedState: Map[String, String]
  )

  class Backend(t: BackendScope[Props, State]) {

    def onTextChange(P: Props)(value: String): Callback =
      t.modState(_.copy(filteredModels = getFilteredModels(value, P.data), offset = 0))

    def onPreviousClick(P: Props): Callback =
      t.modState(s => s.copy(offset = s.offset - s.rowsPerPage), t.state.flatMap( s => P.afterPrevPage(s.offset , s.filteredModels.slice(s.offset , s.offset + s.rowsPerPage )))) //>>  t.state.map(s => s).flatMap( s => P.afterPrevPage(s.offset , s.filteredModels.slice(s.offset, s.offset - s.rowsPerPage) )  )  

    def onNextClick(P: Props): Callback =
      t.modState(s => s.copy(offset = s.offset + s.rowsPerPage), t.state.flatMap( s => P.afterNextPage(s.offset , s.filteredModels.slice(s.offset, s.offset + s.rowsPerPage)))) //>>  t.state.flatMap( s => P.afterNextPage(s.offset , s.filteredModels.slice(s.offset, s.offset + s.rowsPerPage) )  )

    def getFilteredModels(text: String, models: Vector[Model]): Vector[Model] =
      if (text.isEmpty) models
      else models.filter(_.data.mkString.toLowerCase.contains(text.toLowerCase))

    def sort(f: (Model,Model) => Boolean, item: String): Callback = 
      t.modState{ S => 
        val rows = S.filteredModels
        S.sortedState.getOrElse(item, "") match {
          case ASC => S.copy(filteredModels = rows.reverse, sortedState = Map(item -> DSC), offset = 0)
          case DSC => S.copy(filteredModels = rows.sortWith(f), sortedState = Map(item -> ASC), offset = 0)
          case _   => S.copy(filteredModels = rows.sortWith(f), sortedState = Map(item -> ASC), offset = 0)
        }
      }

    def onPageSizeChange(P: Props)(value: String): Callback = 
      t.modState(s => s.copy(rowsPerPage = value.toInt))  >> t.state.flatMap( s => {
        if(value.toInt != s.rowsPerPage)
          P.afterNextPage(s.offset , s.filteredModels.slice(s.offset, s.offset + value.toInt) ) 
        else
          Callback.info("number of rows did not actually change; no need to update listeners")
      })

    def render(P: Props, S: State) =
      <.div(
        P.style.reactTableContainer,
        P.enableSearch ?= ReactSearchBox(
          onTextChange = onTextChange(P) _,
          style = P.searchBoxStyle),
        settingsBar((P, this, S)),
        tableC((P, S, this)),
        Pager(
          S.rowsPerPage,
          S.filteredModels.length,
          S.offset,
          onNextClick(P),
          onPreviousClick(P)
        )
      )
  }

  def getIntSort(key: Int) = (m1: Model, m2: Model) => m1.data(key).asInstanceOf[Int] < m2.data(key).asInstanceOf[Int]

  def getDoubleSort(key: Int) = (m1: Model, m2: Model) => m1.data(key).asInstanceOf[Double] < m2.data(key).asInstanceOf[Double]

  def getLongSort(key: Int) = (m1: Model, m2: Model) => m1.data(key).asInstanceOf[Long] < m2.data(key).asInstanceOf[Long]

  def getStringSort(key: Int) = (m1: Model, m2: Model) => m1.data(key).toString < m2.data(key).toString

  def getDateSort(key: Int) = (m1: Model, m2: Model) => m1.data(key).asInstanceOf[Date].getTime() < m2.data(key).asInstanceOf[Date].getTime()

  def getRenderFunction(key: String, config: List[Config]) = {
    val group = config.groupBy(_._1).getOrElse(key, Nil)
    if (group.nonEmpty) group.head._2 else None
  }

  def getSortFunction(key: String, config: List[Config]) = {
    val group = config.groupBy(_._1).getOrElse(key, Nil)
    if (group.nonEmpty) group.head._3 else None
  }

  def getColumnDiv(key: String, config: List[Config]) = {
    val group = config.groupBy(_._1).getOrElse(key, Nil)
    if (group.nonEmpty && group.head._4.isDefined) <.div(^.flex := group.head._4.get)
    else <.div()
  }
  
  def getOnRowSelect(row:Model, rowSelectOption: Option[(Model) => Callback]) = {
    rowSelectOption match {
      case None => ^.onClick --> Callback.info("no on row select")
      case Some(cb) => ^.onClick --> cb(row)
    } 
  }
  
  def getRowStyler(row:Model, props:Props) = {
    props.rowStyler match {
      case None => props.style.tableRow
      case Some(styler) => styler(row) match {
        case None => props.style.tableRow
        case Some(style) => props.style.idToStyle(style) 
      }
    } 
  }

  def arrowUp: TagMod =
    Seq(
      ^.width := 0,
      ^.height := 0,
      ^.borderLeft := "5px solid transparent",
      ^.borderRight := "5px solid transparent",
      ^.borderBottom := "5px solid black"
    )

  def arrowDown: TagMod =
    Seq(
      ^.width := 0,
      ^.height := 0,
      ^.borderLeft := "5px solid transparent",
      ^.borderRight := "5px solid transparent",
      ^.borderTop := "5px solid black"
    )

  def emptyClass: TagMod =
    Seq(^.padding := "1px")

  val tableHeader = ReactComponentB[(Props, Backend, State)]("reactTableHeader")
    .render{$ =>
      val (props, b, state) = $.props
      <.div(props.style.tableHeader,
        if (props.config.nonEmpty) {
          props.columns.map { item => {
            val cell = getColumnDiv(item,props.config)
            val f    = getSortFunction(item, props.config)
            if (f.isDefined) {
              cell(^.cursor := "pointer",
                ^.onClick --> b.sort(f.get, item), item.capitalize,
                state.sortedState.isDefinedAt(item) ?= props.style.sortIcon(state.sortedState(item) == ASC)
              )
            }
            else cell(item.capitalize)
          }
          }
        }
        else props.columns.map(s => <.div(s.capitalize))
      )
    }.build

  val tableRow = ReactComponentB[(Model, Props)]("TableRow")
    .render{$ =>
      val (row, props) = $.props
      <.div(getRowStyler(row, props),getOnRowSelect(row,props.onRowSelect),
        if (props.config.nonEmpty) {
          props.columns.zipWithIndex.map { item =>
            val cell = getColumnDiv(item._1,props.config)
            val f = getRenderFunction(item._1, props.config)
            if (f.isDefined) cell(f.get(item._1, row.data(item._2), row))
            else cell(row.data(item._2).toString)
          }
        }
        else props.columns.zipWithIndex.map { item => <.div(row.data(item._2).toString)}
      )
    }.build

  val tableC = ReactComponentB[(Props, State, Backend)]("table")
    .render{$ =>
      val (props, state, b) = $.props
      val rows = state.filteredModels
        .slice(state.offset, state.offset + state.rowsPerPage)
        .zipWithIndex.map {
          case (row, i) => tableRow.withKey(i)((row, props))
        }
      <.div(props.style.table,
        tableHeader((props, b, state)),
        rows
      )
    }.build

  val settingsBar = ReactComponentB[(Props, Backend, State)]("settingbar")
    .render{$ =>
      val (p, b, s) = $.props
      var value = ""
      var options: List[String] = Nil
      val total = s.filteredModels.length
      if (total > p.rowsPerPage) {
        value = s.rowsPerPage.toString
        options = immutable.Range.inclusive(p.rowsPerPage, total, 10 * (total / 100 + 1)).:+(total).toList.map(_.toString)
      }
      <.div(p.style.settingsBar)(
        <.div(<.strong("Total: " + s.filteredModels.size)),
        DefaultSelect(label = "Page Size: ",
          options = options,
          value = value,
          onChange = b.onPageSizeChange(p))
      )
    }.build

  val component = ReactComponentB[Props]("ODInTable")
    .initialState_P(p => State(filterText = "", offset = 0, p.rowsPerPage, p.data, Map()))
    .renderBackend[Backend]
    .componentWillReceiveProps(e => Callback.ifTrue(e.$.props.data != e.nextProps.data, e.$.backend.onTextChange(e.nextProps)(e.$.state.filterText)))
    .build

  case class Props(data: Vector[Model],
                   columns: List[String],
                   config: List[Config],
                   afterNextPage: (Int, Vector[Model]) => Callback,
                   afterPrevPage: (Int, Vector[Model]) => Callback,
                   onRowSelect: Option[(Model) => Callback],
                   rowStyler:Option[Model => Option[String]],
                   rowsPerPage: Int,
                   style: Style,
                   enableSearch: Boolean,
                   searchBoxStyle: ReactSearchBox.Style)

  def apply(data: Vector[Model], columns: List[String], config: List[Config] = List(), afterNextPage:(Int, Vector[Model]) => Callback = (offset, rows) => Callback.info("ODOnTable: no after next page"), afterPrevPage:(Int, Vector[Model]) => Callback = (offset, rows) => Callback.info("ODOnTable: no after prev page"), onRowSelect: Option[(Model) => Callback] = None, rowStyler:Option[Model => Option[String]] = None, rowsPerPage: Int = 5, style: Style = DefaultStyle,enableSearch: Boolean = true,searchBoxStyle :ReactSearchBox.Style = ReactSearchBox.DefaultStyle) =
    component(Props(data, columns, config, afterNextPage, afterPrevPage, onRowSelect, rowStyler, rowsPerPage, style,enableSearch,searchBoxStyle))
}
