package ubodin
import scalatags.Text.all._

object UBOdinClientAppPage{
  val boot = "ubodin.Client().main()"
  def skeleton() : scalatags.Text.TypedTag[String] = {
    html(
head(
meta(charset := "UTF-8"),
raw(UBOdinClientAppCSS.cssStr)
//link(rel := "stylesheet", `type` := "text/css", href := "app/assets/slick.css"),
//link(rel := "stylesheet", `type` := "text/css", href := "app/assets/semantic.css")
),
body(
div(id := "container"),
raw(UBOdinClientAppLoadingScript.scriptStr),
script(`type` := "text/javascript", src := "app/bundles/index-bundle.js"),
script(`type` := "text/javascript", src := "js/app-fastopt.js"),
script(`type` := "text/javascript", src := "app/bundles/material_ui-bundle.js"),
script(`type` := "text/javascript", src := "app/bundles/elemental_ui-bundle.js"),
script(`type` := "text/javascript", src := "app/bundles/react_geom_icons-bundle.js"),
script(`type` := "text/javascript", src := "app/bundles/react_infinite-bundle.js"),
script(`type` := "text/javascript", src := "app/bundles/react_select-bundle.js"),
script(`type` := "text/javascript", src := "app/bundles/react_slick-bundle.js"),
script(`type` := "text/javascript", src := "app/bundles/react_spinner-bundle.js"),
script(`type` := "text/javascript", src := "app/bundles/react_tags_input-bundle.js"),
//script(`type` := "text/javascript", src := "app/bundles/semantic_ui-bundle.js"),
script(`type` := "text/javascript", src := "app/bundles/markerclusterer.js"),
script(boot)
)
)
  }
}


object UBOdinClientAppLoadingScript {
  val scriptStr = """<script>loadingElement = document.createElement('div')
    loadingElement.className = 'pg-loading-screen'
    loadingElement.innerHTML = '<div class=\"message\"><blockquote>Loading...</blockquote><div class=\"spinner\"><div class=\"bounce1\"></div><div class=\"bounce2\"></div><div class=\"bounce3\"></div></div></div>'
    document.body.appendChild(loadingElement)
    document.body.className += ' pg-loading'</script>"""
}

object UBOdinClientAppCSS {
  val cssStr = """<style>html {
	font-family:  sans-serif;
	/* 1 */
	-ms-text-size-adjust: 100%;
	/* 2 */
	-webkit-text-size-adjust: 100%;
	/* 2 */
}
body {
	margin: 0;
	font-size: 13px;
	line-height: 20px;
}
a {
	color: red;
	background: transparent;
	text-decoration: none;
}
/*body.pg-loading {*/
	/*overflow: hidden;*/
/*}*/
.pg-loading-screen {
	position: fixed;
	bottom: 0;
	left: 0;
	right: 0;
	top: 0;
	z-index: 1000000;
	opacity: 1;
	/*background-color: #d35400;*/
	background-color: #4286f4;
	-webkit-transition: background-color 0.4s ease-in-out 0s;
	-moz-transition: background-color 0.4s ease-in-out 0s;
	-ms-transition: background-color 0.4s ease-in-out 0s;
	-o-transition: background-color 0.4s ease-in-out 0s;
	transition: background-color 0.4s ease-in-out 0s;
}

.spinner {
	margin: 50px auto 0;
	width: 70px;
	text-align: center;
}

.spinner > div {
	width: 18px;
	height: 18px;
	background-color: white;

	border-radius: 100%;
	display: inline-block;
	-webkit-animation: bouncedelay 1.4s infinite ease-in-out;
	animation: bouncedelay 1.4s infinite ease-in-out;
	/* Prevent first frame from flickering when animation starts */
	-webkit-animation-fill-mode: both;
	animation-fill-mode: both;
}

.spinner .bounce1 {
	-webkit-animation-delay: -0.32s;
	animation-delay: -0.32s;
}

.spinner .bounce2 {
	-webkit-animation-delay: -0.16s;
	animation-delay: -0.16s;
}

@-webkit-keyframes bouncedelay {
	0%, 80%, 100% { -webkit-transform: scale(0.0) }
	40% { -webkit-transform: scale(1.0) }
}

@keyframes bouncedelay {
	0%, 80%, 100% {
		transform: scale(0.0);
		-webkit-transform: scale(0.0);
	} 40% {
		  transform: scale(1.0);
		  -webkit-transform: scale(1.0);
	  }
}
.message {
	text-align: center;
	margin: 100px auto;
	color: #ffffff;
	font-size: 18px;
	font-weight: 500;
}

body > #container {
	display: none;
}
body.pg-loaded > #container {
	display: block;
}
hr{
	border: none;
	border-bottom: solid 1px #e0e0e0;
	margin-top: 0;
	margin-bottom: 18px;
	box-sizing: content-box;
	height: 0;
}</style>"""
}