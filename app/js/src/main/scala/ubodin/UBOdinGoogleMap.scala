package ubodin

import ubodin.fascades._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom.{Event, document, html}

import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global => g}
import ubodin.fascades.LatLng
import ubodin.fascades.GMarker
import ubodin.fascades.GMap
import ubodin.fascades.GInfoWindow
import ubodin.fascades.GAddListener

import org.scalajs.dom.ext.Ajax
import scala.concurrent.ExecutionContext.Implicits.global

object UBOdinGoogleMap {

  def parameterizeUrl(url: String, parameters: Map[String, Any]): String = {
    require(url != null, "Missing argument 'url'.")
    require(parameters != null, "Missing argument 'parameters'.")

    parameters.foldLeft(url)((base, kv) =>
      base ++ {
        if (base.contains("?")) "&" else "?"
      } ++ kv._1 ++ "=" + kv._2)
  }

  case class Address_components(
  long_name: String,
  short_name: String,
  types: List[String]
  )
  case class Location(
    lat: Double,
    lng: Double
  )
  case class Viewport(
    northeast: Location,
    southwest: Location
  )
  case class Geometry(
    location: Location,
    location_type: String,
    viewport: Viewport
  )
  case class Results(
    address_components: List[Address_components],
    formatted_address: String,
    geometry: Geometry,
    place_id: String,
    types: List[String]
  )
  case class GoogleGeocodingResult(
    results: List[Results],
    status: String
  )
  
  def geocodeAddress(houseNumber:String, streetName:String, city:String, state:String, cb:GoogleGeocodingResult => Callback) : Callback = {
    val url = s"https://maps.googleapis.com/maps/api/geocode/json?address=$houseNumber+${streetName.replaceAll(" ", "+")},+${city.replaceAll(" ", "+")},+$state&key=AIzaSyAKc9sTF-pVezJY8-Dkuvw07v1tdYIKGHk"
    println(s"google geocoding request:\n$url")
    Callback.future(Ajax.get(url).map( xhr => {
      cb(upickle.read[GoogleGeocodingResult](xhr.responseText))
    }))
  }
  
  case class State(mapObjects: Option[(GMap, GInfoWindow)], markers: List[GMarker], cluster:Boolean, clusterer:Option[GMarkerClusterer])

  class Backend(t: BackendScope[Props, State]) {
    def loadScript(P: Props): Callback =
      if (js.isUndefined(g.google) || js.isUndefined(g.google.maps))
        Callback {
          val script = document.createElement("script").asInstanceOf[html.Script]
          script.`type` = "text/javascript"
          script.src = parameterizeUrl(P.url, Map("callback" -> "gmapinit"))
          document.body.appendChild(script)
          g.gmapinit = initialize(P).toJsFn
        } else initialize(P)

    def initialize(P: Props): Callback =
      t.modState(
        _.copy(mapObjects = Some((new GMap(t.getDOMNode(), MapOptions(P.center, P.zoom).toGMapOptions), new GInfoWindow)), cluster = P.cluster, clusterer = None),
        cb = updateMap(P)
      )

    def updateMap(P: Props): Callback =
      t.modState(
        S =>
          S.mapObjects.fold(S) {
            case (gmap, infoWindow) =>
              gmap.setOptions(MapOptions(P.center, P.zoom).toGMapOptions)
              S.clusterer match {
                case None => {}
                case Some(existingClusterer) => existingClusterer.clearMarkers()
              }
              S.markers.foreach(_.setMap(null))
              val newMarkers = P.markers.map(prepareMarker(infoWindow, gmap)).toList
              if(P.cluster)
                S.copy(markers = newMarkers, cluster = P.cluster, clusterer = Some(new GMarkerClusterer(gmap, newMarkers.toJsArray, scala.scalajs.js.Dynamic.literal(imagePath = "/app/images/m"))))
              else
                S.copy(markers = newMarkers, cluster = P.cluster, clusterer = None)
          }
      )

    private def prepareMarker(infowindow: GInfoWindow, map: GMap)(m: Marker) = {
      val marker = new GMarker(m.toGMarker(map))
      if (!m.content.isEmpty) {
        new GAddListener(
          marker, "click", (e: Event) => {
            infowindow.setContent(m.content)
            infowindow.open(map, marker)
          }
        )
      }
      if (m.icon != null) {
        marker.setIcon(m.icon.toGIcon)
      }
      
      marker
    }

    def render(P: Props) = <.div(^.height := P.height, ^.width := P.width)
  }

  case class Props(width: String, height: String, center: LatLng, zoom: Int, markers: Seq[Marker], url: String, cluster:Boolean)

  val component = ReactComponentB[Props]("googleMap")
    .initialState(State(None, Nil, false, None))
    .renderBackend[Backend]
    .componentWillReceiveProps{
      case ComponentWillReceiveProps(_$, nextProps) => _$.backend.updateMap(nextProps)
    }
    .componentDidMount($ => $.backend.loadScript($.props))
    .componentWillUnmount($ => Callback($.state.markers.foreach(new GClearInstanceListeners(_))))
    .build

  /**
   *
   * @param width width of map
   * @param height height of map
   * @param center center position(lat,lng) for map
   * @param zoom zoom value
   * @param markers markers for the map
   * @param url url to get googlemap api, by default it uses https://maps.googleapis.com/maps/api/js you can override if you want.
   * @return
   */
  def apply(width: String = "500px",
            height: String = "600px",
            center: LatLng,
            zoom: Int = 4,
            cluster:Boolean = false,
            markers: List[Marker] = Nil,
            url: String = "https://maps.googleapis.com/maps/api/js?key=AIzaSyAKc9sTF-pVezJY8-Dkuvw07v1tdYIKGHk") =
    component(Props(width, height, center, zoom, markers, url, cluster))

}
