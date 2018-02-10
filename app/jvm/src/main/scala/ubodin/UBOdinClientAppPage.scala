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
//raw(UBOdinUserEventTracker.scriptStr),
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

object UBOdinUserEventTracker {
  val scriptStr = """<script>
    var evnts=["click","focus","blur","keyup","keydown","keypressed"];
    // You can also Use mouseup/down, mousemove, resize and scroll
    for(var i=0;i<evnts.length;i++){
      window.addEventListener(""+evnts[i]+"", function(e){ myFunction(e); }, false);
      }
      function myFunction(e){
        var evt=e||window.event;
        if(evt){
        if(evt.isPropagationStopped&&evt.isPropagationStopped()){
            return;
        }
        var et=evt.type?evt.type:evt;
        var trgt=evt.target?evt.target:window;
        var time=Math.floor(Date.now() / 1000);
        var x=0, y=0, scrolltop=0;
        if(evt.pageX){
          x=evt.pageX;
        }
        if(evt.pageY){
          y=evt.pageY;
        }
        if(trgt.scrollTop){
          scrolltop=trgt.scrollTop;
        }
        if(trgt.className&&trgt.id){
          trgt="."+trgt.className+"#"+trgt.id;
        }
        else if(trgt.id){
          trgt="#"+trgt.id;
        }
        else if(trgt.className){
          trgt="."+trgt.className;
        }
        
        if(typeof(trgt)!="String"){
            if(trgt.tagName){
             trgt=trgt.tagName;
            }
            else{
            trgt=trgt.toString().toLowerCase();
            trgt=trgt.replace("[object ","");
            trgt=trgt.replace("]","");
                trgt=trgt.replace("htmlbodyelement","BODY");
            }
        }
        var xtra="";
        if(evt.keyCode){
          xtra+=" KeyCode: "+evt.keyCode;
        }
        if(evt.shiftKey){
          xtra+=" ShiftKey ";
        }
        if(evt.altKey){
          xtra+=" altKey ";
        }
        if(evt.metaKey){
          xtra+=" metaKey ";
        }
        if(evt.ctrlKey){
          xtra+=" ctrlKey ";
        }
        var w = window,
            d = document,
            e = d.documentElement,
            g = d.getElementsByTagName('body')[0],
            xx = w.innerWidth || e.clientWidth || g.clientWidth,
            yy = w.innerHeight|| e.clientHeight|| g.clientHeight;
        xtra+=" RES:"+xx+"X"+yy;
        
        //console.log("EventType: "+et+" Target:"+trgt+" X Coord: "+x+" Y Coord: "+y+" Scroll: "+scrolltop+" Timestamp:"+time+" Res: "+xtra+"");
        ubodin.SendUserActionLog().send(et, trgt, x, y, scrolltop,time,xtra);
      }
    }
    </script>"""
}

object UBOdinPlotGenerator {
  
  def testplot(): String = {
    var datatsv = "["
    var randVal = Math.random()
    var pow = 3.0
    for(i <- 1 to 1200 by 8){ 
      pow = 3.0 + (Math.random()/50.0)
      if(randVal < 0.3 && randVal > 0.2)
        randVal =  1.0 + (Math.random()/50.0)
      else if(randVal > 0.9)
        randVal =  1.0 - (Math.random()/60.0)
      else if(randVal < 0.1)
        randVal =  1.0 + (Math.random()/30.0)
      else if(randVal < 0.2)
        randVal = 1.0 - (Math.random()/20.0)
      else
        randVal = 1.0 + (Math.random()/20.0)
      datatsv+=s"\n{xval:${i}, yval:${Math.pow((randVal*i.toDouble)/500.0,pow)}}${if(i==1193) "" else ","}"
      randVal = Math.random()
    }
    datatsv += "]"
    getPlotHtml(datatsv,"curveStepAfter","Test Plot", "Date", "Close")
  }
  
  
  def getd3():String = {
  ""
  }
  
  def getPlotHtml(data : String, curveType:String, title:String, xaxisLabel:String = "X Axis", yaxisLabel:String = "Y Axis", lineColor:String = "#ffab00"): String = {
    """<html>
    <head>
    <meta charset="utf-8">
    <title>"""+title+"""</title>
    <style>
      html,body, {
          width: 100%;
          height: 100%;
      }
      
      #graph {
        width: calc(100% - 10px);
        height: calc(100% - 10px);
      }
      
      .valueLine{
          fill:none;
          stroke:"""+lineColor+""";
      }
      
      .valueErr{
          fill:red;
      }
      
      .tick line {
          stroke-dasharray: 2 2 ;
          stroke: #ccc;
      }
    </style>
    </head>
    <body>
    <div id="graph"></div>  
    <script src="/app/assets/d3.min.js"></script>
    <script>
    !(function(){
        "use strict"
        
        var width,height
        var chartWidth, chartHeight
        var margin
        var svg = d3.select("#graph").append("svg")
        var axisLayer = svg.append("g").classed("axisLayer", true)
        var chartLayer = svg.append("g").classed("chartLayer", true)
        
        var xScale = d3.scaleLinear()
        var yScale = d3.scaleLinear()
        
        
        var jsonData = """+data+""";
        
        var xMin = d3.min(jsonData, function(d){ return d.xval.val})
        var yMin = d3.min(jsonData, function(d){ return d.yval.val})
        var xMax = d3.max(jsonData, function(d){ return d.xval.val})
        var yMax = d3.max(jsonData, function(d){ return d.yval.val})
        yMin = yMin - (yMax*0.1)
        yMax = yMax + (yMax*0.1)
        
        main(jsonData);
                
        function main(data) {
            update(data)
            setReSizeEvent(data)
        }
        
        
        function update(data) {
            setSize(data)
            drawAxis()
            drawChart(data)        
        }
    
        function setReSizeEvent(data) {
            var resizeTimer;
            var interval = Math.floor(1000 / 60 * 10);
             
            window.addEventListener('resize', function (event) {
                
                if (resizeTimer !== false) {
                    clearTimeout(resizeTimer);
                }
                resizeTimer = setTimeout(function () {
                    update(data)
                }, interval);
            });
        }
        
        
        function setSize(data) {
            width = document.querySelector("#graph").clientWidth
            height = document.querySelector("#graph").clientHeight
        
            margin = {top:40, left:60, bottom:40, right:60 }
            
            chartWidth = width - (margin.left+margin.right)
            chartHeight = height - (margin.top+margin.bottom)
            
            svg.attr("width", width).attr("height", height)
            axisLayer.attr("width", width).attr("height", height)
            
            chartLayer
                .attr("width", chartWidth)
                .attr("height", chartHeight)
                .attr("transform", "translate("+[margin.left, margin.top]+")")
                
            
                
            xScale.domain([xMin, xMax]).range([0, chartWidth])
            yScale.domain([yMin, yMax]).range([chartHeight, 0])
                
        }
        
        function filterJson(collection, predicate)
        {
            var result = new Array();
            var length = collection.length;
            for(var j = 0; j < length; j++)
            {
                if(predicate(collection[j]) == true)
                {
                     result.push(collection[j]);
                }
            }
            console.log(result);
            return result;
        }
        
        function drawChart(data) {
            var t = d3.transition()
                .duration(1000)
                .ease(d3.easeLinear)
                .on("start", function(d){ console.log("transiton start") })
                .on("end", function(d){ console.log("transiton end") })
            
            
            var lineGen = d3.line()
                .x(function(d) { return xScale(d.xval.val) })
                .y(function(d) { return yScale(d.yval.val) })
                .curve(d3."""+curveType+""")
               
            var selectedLineElm = chartLayer.selectAll(".valueLine")
                .data([data])
            
            var newLineElm = selectedLineElm.enter().append("path")
                .attr("class", "valueLine")
    
            selectedLineElm.merge(newLineElm)
                .attr("d", lineGen)
            
            
            var selectedDotElm = chartLayer.selectAll(".valueErr")
                .data(filterJson(data, function(element){ return !element.yval.det }))
            
            var newDotElm = selectedDotElm.enter().append("circle")  
                .attr("r", 2)           
                .attr("cx", function(d) { return xScale(d.xval.val); })       
                .attr("cy", function(d) { return yScale(d.yval.val); })
                .attr("class", "valueErr")  
    
            selectedDotElm.merge(newDotElm)
                       
        }
        
        function drawAxis(){
            var yAxis = d3.axisLeft(yScale)
                .tickSizeInner(-chartWidth)
            
            var selectedYAxisElm = axisLayer.selectAll(".y")
                .data(["dummy"])
                
            var newYAxisElm = selectedYAxisElm.enter().append("g")
                .attr("class", "axis y")
                
            selectedYAxisElm.merge(newYAxisElm)
                .attr("transform", "translate("+[margin.left, margin.top]+")")
                .call(yAxis);
                
            var xAxis = d3.axisBottom(xScale)
        
            var selectedXAxisElm = axisLayer.selectAll(".x")
                .data(["dummy"])
                
            var newXAxisElm = selectedXAxisElm.enter().append("g")
                .attr("class", "axis x")
        
            selectedXAxisElm.merge(newXAxisElm)
                .attr("transform", "translate("+[margin.left, chartHeight+margin.top]+")")
                .call(xAxis);
            
        }
    }());
    </script>  
    </body>
    </html>
    """
  }
}