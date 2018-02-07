package ubodin

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.charset.Charset
import scala.xml.XML


object VistrailsClasses {

  def convertVtToMx(vtWorkflow:Workflow) : MxWorkflow = {
     val portsConn = vtWorkflow.connection.map(conn => {
      val (srcPorts, dstPorts) = conn.port.map(port => {
        /*<mxCell id="3" value="" vertex="1" parent="2">
      			<mxGeometry x="1" y="1" width="10" height="10" relative="1" as="geometry">
        			<mxPoint x="-5" y="-5" as="offset"/>
      			</mxGeometry>
    			</mxCell>*/
        if(port.`type`.equals("source"))
          (Some(MxCell(port.id, "", Some("1"),None,Some(port.moduleId),None,None,None,None,
            Some(MxGeometry(Some("1"),Some("1"),Some("10"),Some("10"),Some("1"), "geometry",
              Some(MxPoint("-6","-4","offset")))))), None)
        else if(port.`type`.equals("destination"))
          (None, Some(MxCell(port.id, "", Some("1"),None,Some(port.moduleId),None,None,None,None,
            Some(MxGeometry(Some("12"),Some("12"),Some("10"),Some("10"),Some("1"), "geometry",
              Some(MxPoint("-8","-4","offset")))))))
        else (None, None)
        
      }).unzip
      val ports = srcPorts.flatMap(el => el) ++ dstPorts.flatMap(el => el)
      val connection = ports match {
        case Seq(sPort:MxCell, dPort:MxCell) => {
          /*<mxCell id="7" value="" style="sourcePort=3;" edge="1" parent="1" source="2" target="5">
            <mxGeometry relative="1" as="geometry"/>
          </mxCell>
          <mxCell id="8" value="" style="sourcePort=4;" edge="1" parent="1" source="2" target="6">
            <mxGeometry relative="1" as="geometry"/>
          </mxCell>*/
         MxCell(conn.id + "_1", "", Some("1"),None,Some("1"),Some(s"sourcePort=${sPort.id};destinationPort=${dPort.id}"),Some("1"),sPort.parent,dPort.parent,
          Some(MxGeometry(None,None,None,None,Some("1"), "geometry", None)))
        }
        case _ => throw new Exception("ports not connected")
      }
      ports :+ connection
    }).flatten
    val modules = vtWorkflow.module.map(module => {
      /*<mxCell id="2" value="Hello" vertex="1" connectable="0" parent="1">
          <mxGeometry x="20" y="80" width="80" height="30" as="geometry"/>
        </mxCell>*/
      MxCell(module.id, module.name, Some("1"),Some("0"),Some("1"),None,None,None,None,
        Some(MxGeometry(Some(module.location.x),Some(module.location.y),Some("80"),Some("30"),None, "geometry", None)))
    })
    MxWorkflow(modules++portsConn)
  }
  def convertMxToVt(mxWorkflow:MxWorkflow) : Workflow = {
    val (connections, modules) = mxWorkflow.cells.map( cell => cell match {
      case MxCell(id,"connection",vertex, connectable, parent, style, edge, source, target, mxGeometry) => 
        (Some(Connection(id, List())), None:Option[Module])
    } ).unzip
    Workflow("", "", "", "", connections.flatMap( el => el), modules.flatMap(el => el))
  }
  def mxWorkflowToXml(mxWorkflow:MxWorkflow) = mxWorkflow.toXml().toString()
     
  def vtWorkflowToXml(vtWorkflow:Workflow) : String = {
    ""
  }
  def xmlToMxWorkflow(xml:String) : MxWorkflow = {
    null
  }
  def xmlToVtWorkflow(xml:String) : Workflow = {
    Workflow.fromXml(scala.xml.XML.loadString(xml))
  }
  def loadVtWorkflowFromXmlFile(file:String) : Workflow = {
    xmlToVtWorkflow(new String(Files.readAllBytes(Paths.get(file)), Charset.forName("UTF-8")))
  }
}

case class Port(
  id: String,
  moduleId: String,
  moduleName: String,
  name: String,
  signature: String,
  `type`:String
)
object Port {
  def fromXml(node: scala.xml.Node) : Port = {
    Port(
      (node \ "@id").text,
      (node \ "@moduleId").text,
      (node \ "@moduleName").text,
      (node \ "@name").text,  
      (node \ "@signature").text,
      (node \ "@type").text
    )
  }
}
case class Connection(
  id: String,
  port: List[Port]
)
object Connection {
  def fromXml(node: scala.xml.Node) : Connection ={
    Connection(
        (node \ "@id").text,
        ((node \ "port") map(n => Port.fromXml(n))).toList)
  }
}
case class Location(
  id: String,
  x: String,
  y: String
)
object Location {
  def fromXml(node: scala.xml.Node) : Location = {
    Location(
        (node \ "@id").text,
        Math.abs((node \ "@x").text.toDouble).toString(),
        Math.abs((node \ "@y").text.toDouble).toString())
  }
}
case class Parameter(
  alias: String,
  id: String,
  name: String,
  pos: String,
  `val`: String
)
object Parameter {
  def fromXml(node: scala.xml.Node) : Parameter = {
    Parameter(
        (node \ "@alias").text,
        (node \ "@id").text,
        (node \ "@name").text,
        (node \ "@pos").text,
        (node \ "@val").text)
  }
}
case class Function(
  id: String,
  name: String,
  pos: String,
  parameter: Parameter
)
object Function {
  def fromXml(node: scala.xml.Node) : Function = {
    Function(
        (node \ "@id").text,
        (node \ "@name").text,
        (node \ "@pos").text,
        ((node \ "parameter") map(n => Parameter.fromXml(n))).head)
  }
}
case class Module(
  cache: String,
  id: String,
  name: String,
  namespace: String,
  `package`: String,
  version: String,
  location: Location,
  function: List[Function]
)
object Module {
  def fromXml(node: scala.xml.Node) : Module = {
    Module(
        (node \ "@cache").text,
        (node \ "@id").text,
        (node \ "@name").text,
        (node \ "@namespace").text,
        (node \ "@package").text,
        (node \ "@version").text,
        ((node \ "location") map(n => Location.fromXml(n))).head,
        ((node \ "function") map(n => Function.fromXml(n))).toList)
  }
}
case class Workflow(
  id: String,
  name: String,
  version: String,
  vistrail_id: String,
  connection: List[Connection],
  module: List[Module]
)
object Workflow {
  def fromXml(node: scala.xml.Node): Workflow =
    Workflow(
      (node \ "@id").text,
      (node \ "@name").text,
      (node \ "@version").text,
      (node \ "@vistrail_id").text,
      ((node \ "connection") map(n => Connection.fromXml(n))).toList,
      ((node \ "module") map(n => Module.fromXml(n))).toList
    )
}

///////////////////////////////////////////
//mxGraph classes
///////////////////////////////
case class MxPoint(
  x: String,
  y: String,
  as: String
){
  def toXml() = <mxPoint x={x} y={y} as={as} />
}
case class MxGeometry(
  x: Option[String],
  y: Option[String],
  width: Option[String],
  height: Option[String],
  relative: Option[String],
  as: String,
  mxPoint: Option[MxPoint]
){
  def toXml() = <mxGeometry 
		x={x.getOrElse(null)} 
  	y={y.getOrElse(null)} 
 		width={width.getOrElse(null)} 
  	height={height.getOrElse(null)} 
  	relative={relative.getOrElse(null)} 
  	as={as} >
		{ mxPoint match {
    		  case Some(mxP) => mxP.toXml
    		  case None => null
    		}}
	</mxGeometry>
}
case class MxCell(
  id: String,
  value: String,
  vertex: Option[String],
  connectable: Option[String],
  parent: Option[String],
  style: Option[String],
  edge: Option[String],
  source: Option[String],
  target: Option[String],
  mxGeometry: Option[MxGeometry]
){
  def toXml() = 
    <mxCell 
			id={id} 
			value={value} 
			vertex={vertex.getOrElse(null)} 
			connectable={connectable.getOrElse(null)}
  		parent={parent.getOrElse(null)}
  		style={style.getOrElse(null)}
  		edge={edge.getOrElse(null)}
  		source={source.getOrElse(null)}
  		target={target.getOrElse(null)} >
		  { mxGeometry match {
    		  case Some(mxGeom) => mxGeom.toXml
    		  case None => null
    		}}
		</mxCell>
  
}
case class MxWorkflow(
  cells:List[MxCell]    
){
  def toXml() = 
    <mxGraphModel>
  		<root>
    		<mxCell id="0"/>
    		<mxCell id="1" parent="0"/>
				{cells.map(cell => cell.toXml())}
		  </root>
    </mxGraphModel>
}