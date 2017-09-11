package ubodin
import scalikejdbc._
import ubodin.Base64.Encoder

import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.SecureRequestCustomizer
import org.eclipse.jetty.server.SslConnectionFactory
import org.eclipse.jetty.server.HttpConfiguration
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.http.HttpVersion
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.server.handler.ResourceHandler
import org.eclipse.jetty.server.handler.ContextHandler
import org.eclipse.jetty.server.handler.DefaultHandler
import org.eclipse.jetty.server.handler.HandlerCollection
import org.eclipse.jetty.server.Handler
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.util.ArrayList
import upickle._
import java.io.InputStream
import java.io.ByteArrayInputStream
import java.sql.PreparedStatement
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletResponse

import mimir.MimirVizier
import mimir.exec.result.Row
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}

class MainServlet() extends HttpServlet {
  override def doPost(req : HttpServletRequest, resp : HttpServletResponse) = {
      val text = scala.io.Source.fromInputStream(req.getInputStream).mkString 
      println(req.getPathInfo)
      val routePattern = "\\/ajax\\/([a-zA-Z]+)".r
      req.getPathInfo match {
        case routePattern(route) => {
          val responseObj = handleRequest(route, text)
          val os = resp.getOutputStream()
          resp.setHeader("Content-type", "text/html");
          os.write(responseObj.getBytes )
          os.flush()
          os.close() 
        }
        case _ => throw new Exception("request Not handled: " + req.getPathInfo)
      }  
  }
  override def doGet(req : HttpServletRequest, resp : HttpServletResponse) = {
    println(req.getPathInfo)
    var responseStr = UBOdinClientAppPage.skeleton().render;
    if(req.getPathInfo.matches("\\/genpdf\\/?")){
      if(req.getParameter("task") != null){
       
      }
    }
   
    val os = resp.getOutputStream()
            resp.setHeader("Content-type", "text/html");
            os.write(responseStr.getBytes() )
            os.flush()
            os.close()
            println("Response Written: " + responseStr.length())
  }
  
  
  def handleRequest(route:String, request:String) : String = {
   
    val requestObj = UBOdinAjaxRequest.readRequest(route, request)
          
    requestObj match {
      case CleaningJobListRequest(deviceID) =>  {
        val cleaningJobs = getCleaningJobList(deviceID)
        upickle.write(CleaningJobListResponse(Vector[CleaningJob]().union(cleaningJobs)))
      }
      case CleaningJobTaskListRequest(deviceID, cleaningJobID, count, offset) =>  {
        val cleaningJobs = getCleaningJobTaskList(deviceID, cleaningJobID, count, offset)
        upickle.write(CleaningJobTaskListResponse(Vector[CleaningJobTaskGroup]().union(cleaningJobs)))
      }
      case CleaningJobTaskFocusedListRequest(deviceID, cleaningJobID, rowIds, cols) =>  {
        val cleaningJobs = getCleaningJobTaskFocusedList(deviceID, cleaningJobID, rowIds, cols)
        upickle.write(CleaningJobTaskListResponse(Vector[CleaningJobTaskGroup]().union(cleaningJobs)))
      }
      case CleaningJobDataRequest(deviceID, cleaningJobID, count, offset) =>  {
        val cleaningJobData = getCleaningJobData(deviceID, cleaningJobID, count, offset)
        upickle.write(CleaningJobDataResponse(cleaningJobData))
      }
      case filterReq@CreateCleaningJobLocationFilterSettingRequest(deviceID, cleaningJobDataID, name, distance, latCol, lonCol, lat, lon) =>  {
        createCleaningJobLocationFilterSetting(filterReq)
        upickle.write(NoResponse())
      }
      case filterReq@RemoveCleaningJobLocationFilterSettingRequest(deviceID, cleaningJobDataID, name) =>  {
        removeCleaningJobLocationFilterSetting(filterReq)
        upickle.write(NoResponse())
      }
      case GetCleaningJobSettingsRequest(deviceID, cleaningJobID) =>  {
        val cleaningJobSettings = getCleaningJobSettings(deviceID, cleaningJobID)
        upickle.write(GetCleaningJobSettingsResponse(cleaningJobSettings.toVector))
      }
      case GetCleaningJobSettingsOptionsRequest(cleaningJobID) =>  {
        val cleaningJobSettingsOptions = getCleaningJobSettingsOptions(cleaningJobID)
        upickle.write(GetCleaningJobSettingsOptionsResponse(cleaningJobSettingsOptions.toVector))
      }
      case LoadCleaningJobRequest(deviceID, cleaningJobID) =>  {
        val cleaningJobSettingsOptions = getCleaningJobSettingsOptions(cleaningJobID)
        val options = GetCleaningJobSettingsOptionsResponse(cleaningJobSettingsOptions.toVector)
        val cleaningJobSettings = getCleaningJobSettings(deviceID, cleaningJobID)
        val settings = GetCleaningJobSettingsResponse(cleaningJobSettings.toVector)
        val cleaningJobTasks = getCleaningJobTaskList(deviceID, cleaningJobID, 10, 0)
        val tasks = CleaningJobTaskListResponse(Vector[CleaningJobTaskGroup]().union(cleaningJobTasks))
        val cleaningJobData = getCleaningJobData(deviceID, cleaningJobID, None, None)
        val data = CleaningJobDataResponse(cleaningJobData)
        upickle.write(LoadCleaningJobResponse(getCleaningJob(cleaningJobID), options, settings, tasks, data))
      }
      case UserInfoRequest(deviceID) => {
        upickle.write(UserInfoResponse(getUserInfo(deviceID).head))
      }
      case SetDeviceLocationRequest(deviceID, lat, lon) => {
        setDeviceLocation(deviceID, lat, lon) 
        upickle.write(NoResponse())
      }
      case LoginRequest(deviceID) => {
        val cleaningJobs = getCleaningJobList(deviceID)
        upickle.write(LoginResponse(UserInfoResponse(getUserInfo(deviceID).head), CleaningJobListResponse(Vector[CleaningJob]().union(cleaningJobs))))
      }
      case CleaningJobRepairRequest(deviceID, cleaningJobID, model, idx, args, repairValue) => {
        println(s"Repair: device:$deviceID, job:$cleaningJobID, model:$model, idx:$idx, repair:$repairValue")
        MimirCommunityServer.feedback(model, idx, args, repairValue)
         upickle.write(NoResponse())
      }
      case x => {
        throw new Exception("request Not handled: " + x)
      }
    }
  }
  
  def getCleaningJobList(deviceID:String) : List[CleaningJob] = {
     NamedDB("mimir_community_server") autoCommit { implicit session =>
         sql"select * from CLEANING_JOBS" // don't worry, prevents SQL injection
          .map(rs => CleaningJob(rs.int("CLEANING_JOB_ID"), rs.string("CLEANING_JOB_NAME"), rs.string("TYPE"), rs.string("IMAGE"), Stream(rs.string("CLEANING_JOB_NAME").toLowerCase())))
          .list                  // single, list, traversable
          .apply() 
        }
  }
  
  def getCleaningJob(cleaningJobID:String) : CleaningJob = {
     NamedDB("mimir_community_server") autoCommit { implicit session =>
         sql"select * from CLEANING_JOBS WHERE CLEANING_JOB_ID = $cleaningJobID" // don't worry, prevents SQL injection
          .map(rs => CleaningJob(rs.int("CLEANING_JOB_ID"), rs.string("CLEANING_JOB_NAME"), rs.string("TYPE"), rs.string("IMAGE"), Stream(rs.string("CLEANING_JOB_NAME").toLowerCase())))
          .single                  // single, list, traversable
          .apply().get 
        }
  }
  
  def getUserInfo(deviceID:String) : List[UserInfoData] = {
     val usersData = NamedDB("mimir_community_server") autoCommit { implicit session =>
         sql"select * from USERS u where u.USER_ID = (SELECT USER_ID from USER_DEVICES where DEVICE_ID = (SELECT DEVICE_ID FROM DEVICES WHERE DEVICE_UID = ${deviceID}) ORDER BY USER_DEVICE_ID desc LIMIT 1)" // don't worry, prevents SQL injection
          .map(rs => {
            val userSchedule = UserSchedule((NamedDB("mimir_community_server") autoCommit { implicit session =>
             sql"SELECT * FROM SCHEDULE_USERS su join SCHEDULE_CLEANING_JOBS sjs on sjs.SCHEDULE_CLEANING_JOBS_ID = su.SCHEDULE_CLEANING_JOBS_ID join CLEANING_JOBS js on js.CLEANING_JOB_ID = sjs.CLEANING_JOB_ID where su.USER_ID = ${rs.string("USER_ID")}"
              .map(rssub => {
                val startedTime = rssub.date("STARTED_TIME") match { case null => 0.0; case x => x.getTime.toDouble; }
                val endedTime = rssub.date("ENDED_TIME")  match { case null => 0.0; case x => x.getTime.toDouble; }
                UserScheduleItem(rssub.int("USER_ID"), CleaningJob(rssub.int("CLEANING_JOB_ID"), rssub.string("CLEANING_JOB_NAME"), rssub.string("TYPE"), rssub.string("IMAGE"), Stream(rssub.string("CLEANING_JOB_NAME").toLowerCase())), rssub.int("SCHEDULE_CLEANING_JOBS_ID"), /*rssub.date("START_TIME").getTime.toDouble, rssub.date("END_TIME").getTime.toDouble*/0, 0, startedTime, endedTime)
              })
              .list                  // single, list, traversable
              .apply() 
            }).toArray )
            val userDevices = (NamedDB("mimir_community_server") autoCommit { implicit session =>
             sql"select d.DEVICE_UID from  USER_DEVICES ud join DEVICES d on d.DEVICE_ID = ud.DEVICE_ID where ud.USER_ID = ${rs.string("USER_ID")}" // don't worry, prevents SQL injection
              .map(rssubd => rssubd.string("DEVICE_UID"))
              .list                  // single, list, traversable
              .apply() 
             }).toArray
            UserInfoData(rs.int("USER_ID"), rs.string("FIRST_NAME"), rs.string("LAST_NAME"), userDevices, userSchedule)
          })
          .list                  // single, list, traversable
          .apply() 
        } 
     if(usersData.isEmpty){
       //save new unregistered device UID
       if((NamedDB("mimir_community_server") autoCommit { implicit session =>
         sql"select d.DEVICE_UID FROM DEVICES d where d.DEVICE_UID = ${deviceID}" // don't worry, prevents SQL injection
          .map(rssubd => rssubd.string("DEVICE_UID"))
          .list                  // single, list, traversable
          .apply() 
         }).isEmpty){
         NamedDB("mimir_community_server") autoCommit  { implicit session =>
           sql"INSERT INTO DEVICES (DEVICE_ID, DEVICE_UID) VALUES(0, ${deviceID})" // don't worry, prevents SQL injection
            .execute                
            .apply() 
           }
       }
     }
     usersData
  }
  
  def getCleaningJobTaskList(deviceID:String, cleaningJobID:String, count:Int, offset:Int) : List[CleaningJobTaskGroup] = {
    val reasonSeqList = NamedDB("mimir_community_server") autoCommit { implicit session =>
         sql"select cjd.*, cj.TYPE from CLEANING_JOB_DATA cjd join CLEANING_JOBS cj on cj.CLEANING_JOB_ID = cjd.CLEANING_JOB_ID WHERE cj.CLEANING_JOB_ID = $cleaningJobID" // don't worry, prevents SQL injection
          .map(rs => {
            MimirCommunityServer.explainEverything(assembleCleaningJobDataQuery(deviceID, rs.int("CLEANING_JOB_DATA_ID"))).map(reasonSet => {
               reasonSet.take(MimirVizier.db, count, offset).map(reason => { 
                 val containedRepair = reason.repair match {
                   case mimir.ctables.RepairFromList(_) => CleaningJobTaskRepair.containRepair("RepairFromList", upickle.read[CleaningJobTaskRepairFromList](reason.repair.toJSON))
                   case mimir.ctables.RepairByType(_) => CleaningJobTaskRepair.containRepair("RepairByType", upickle.read[CleaningJobTaskRepairByType](reason.repair.toJSON))
                 }
                 CleaningJobTaskGroup( List(CleaningJobTask(reason.model.name, reason.idx, reason.reason, containedRepair, reason.args.map( _.toString).toList )))
               })
             }).flatten
          }).list.apply()
        }
        reasonSeqList.flatten
   }
  
  def getCleaningJobTaskFocusedList(deviceID:String, cleaningJobID:String, rows:Seq[String], cols:Seq[String]) : List[CleaningJobTaskGroup] = {
    val reasonSeqList = NamedDB("mimir_community_server") autoCommit { implicit session =>
         sql"select cjd.*, cj.TYPE from CLEANING_JOB_DATA cjd join CLEANING_JOBS cj on cj.CLEANING_JOB_ID = cjd.CLEANING_JOB_ID WHERE cj.CLEANING_JOB_ID = $cleaningJobID" // don't worry, prevents SQL injection
          .map(rs => {
            rs.string("TYPE") match {
              case "GIS" => {
                //for gis we want to group the lat and lon repairs so the can be repaired in one go, so the cols here should be the lat and lon cols
                rows.map(row => CleaningJobTaskGroup(MimirCommunityServer.explainSubset(assembleCleaningJobDataQuery(deviceID, rs.int("CLEANING_JOB_DATA_ID")), Seq(row), cols).map(reasonSet => {
                 reasonSet.all(MimirVizier.db).map(reason => { 
                   //upickle.read[CleaningJobTask](reason.toJSON)
                   val containedRepair = reason.repair match {
                     case mimir.ctables.RepairFromList(_) => CleaningJobTaskRepair.containRepair("RepairFromList", upickle.read[CleaningJobTaskRepairFromList](reason.repair.toJSON))
                     case mimir.ctables.RepairByType(_) => CleaningJobTaskRepair.containRepair("RepairByType", upickle.read[CleaningJobTaskRepairByType](reason.repair.toJSON))
                   }
                   CleaningJobTask(reason.model.name, reason.idx, reason.reason, containedRepair, reason.args.map( _.toString).toList )
                 })
               }).flatten.toList))
              }
              case "DATA" => {
                //for this, each reason becomes it's own task group
                MimirCommunityServer.explainSubset(assembleCleaningJobDataQuery(deviceID, rs.int("CLEANING_JOB_DATA_ID")), rows, cols).map(reasonSet => {
                 reasonSet.all(MimirVizier.db).map(reason => { 
                   //CleaningJobTaskGroup( List(upickle.read[CleaningJobTask](reason.toJSON)))
                   val containedRepair = reason.repair match {
                     case mimir.ctables.RepairFromList(_) => CleaningJobTaskRepair.containRepair("RepairFromList", upickle.read[CleaningJobTaskRepairFromList](reason.repair.toJSON))
                     case mimir.ctables.RepairByType(_) => CleaningJobTaskRepair.containRepair("RepairByType", upickle.read[CleaningJobTaskRepairByType](reason.repair.toJSON))
                   }
                   CleaningJobTaskGroup( List(CleaningJobTask(reason.model.name, reason.idx, reason.reason, containedRepair, reason.args.map( _.toString).toList )))
                 })
               }).flatten
              }
            }
          }).list.apply()
        }
        reasonSeqList.flatten
   }
  
  def getCleaningJobData(deviceID:String, cleaningJobID:String, count:Option[Int], offset:Option[Int]) : Vector[CleaningJobData] = {
    val limitOffset = offset match {
      case Some(offsetIdx) => s" OFFSET $offsetIdx"
      case None => ""
    }
    val limit = count match {
      case Some(limitCount) => s" LIMIT $limitCount$limitOffset"
      case None => ""
    }
    val cleaningJobData = NamedDB("mimir_community_server") autoCommit { implicit session =>
         sql"select * from CLEANING_JOB_DATA WHERE CLEANING_JOB_ID = $cleaningJobID"// don't worry, prevents SQL injection
           .map(rs => {
            val queryRes = MimirCommunityServer.queryMimir(assembleCleaningJobDataQuery(deviceID, rs.int("CLEANING_JOB_DATA_ID")))
            CleaningJobData(rs.int("CLEANING_JOB_DATA_ID"), rs.int("CLEANING_JOB_ID"), queryRes._1, queryRes._2)
          }).list.apply()
        }
        cleaningJobData.toVector
  }
  
  def assembleCleaningJobDataQuery(deviceID:String, cleaningJobDataID:Int) : String = {
    NamedDB("mimir_community_server") autoCommit { implicit session =>
      sql"select * from CLEANING_JOB_DATA WHERE CLEANING_JOB_DATA_ID = $cleaningJobDataID" // don't worry, prevents SQL injection
        .map(rs => {
          val userCleaningJobSettings = NamedDB("mimir_community_server") autoCommit { implicit session =>
           sql"Select s.* from DEVICES d join USER_DEVICES ud on ud.DEVICE_ID = d.DEVICE_ID join USERS u on u.USER_ID = ud.USER_ID join USER_CLEANING_JOB_SETTINGS us on us.USER_ID = u.USER_ID join CLEANING_JOB_SETTINGS s on s.CLEANING_JOB_SETTINGS_ID = us.CLEANING_JOB_SETTING_ID where d.DEVICE_UID = $deviceID AND s.CLEANING_JOB_DATA_ID = ${rs.int("CLEANING_JOB_DATA_ID")}"
            .map(rssub => {
              val settingType = rssub.string("TYPE") 
              val settingJson = rssub.string("SETTING")
              CleaningJobSetting.readSetting(settingType, settingJson)
            })
            .list 
            .apply() 
          }
          userCleaningJobSettings.foldLeft(rs.string("QUERY") + " WHERE 1 = 1")((init, setting) => {
            val settingPart = setting match {
              case CleaningJobFilterSetting(_, _, queryWhere) => s" AND $queryWhere"
              case _ => ""
            }
            init + settingPart
          })
        }).single.apply().get
     }
  }
  
  def createCleaningJobLocationFilterSetting(filterRequest: CreateCleaningJobLocationFilterSettingRequest) : (Long, Long) = {
    MimirCommunityServer.setLocationMimir(filterRequest.lat, filterRequest.lon)
    val filterSetting = upickle.write(CleaningJobFilterSetting(0, filterRequest.cleaningJobDataID.toInt, s"DST(CAST(${filterRequest.latCol} AS real), CAST(${filterRequest.lonCol} AS real), ${filterRequest.lat}, ${filterRequest.lon}) <= ${filterRequest.distance}"))
    NamedDB("mimir_community_server") autoCommit  { implicit session =>
       val cleaningJobSettingsID = sql"INSERT INTO CLEANING_JOB_SETTINGS ( CLEANING_JOB_DATA_ID, TYPE, NAME, SETTING) VALUES(${filterRequest.cleaningJobDataID}, ${"FILTER"}, ${filterRequest.name}, $filterSetting)" // don't worry, prevents SQL injection
        .updateAndReturnGeneratedKey 
        .apply() 
       val userID = sql"SELECT USER_ID from USER_DEVICES where DEVICE_ID = (SELECT DEVICE_ID FROM DEVICES WHERE DEVICE_UID = ${filterRequest.deviceID}) ORDER BY USER_DEVICE_ID desc LIMIT 1"
        .map(rs => { rs.int("USER_ID") })
        .single
        .apply().get
       val userCleaningJobSettingsID = sql"INSERT INTO USER_CLEANING_JOB_SETTINGS (USER_ID, CLEANING_JOB_SETTING_ID) VALUES( ${userID}, ${cleaningJobSettingsID})" // don't worry, prevents SQL injection
        .updateAndReturnGeneratedKey 
        .apply()   
        (cleaningJobSettingsID, userCleaningJobSettingsID)
     }
  }
  
  def removeCleaningJobLocationFilterSetting(filterRequest: RemoveCleaningJobLocationFilterSettingRequest) : Unit = {
    NamedDB("mimir_community_server") autoCommit  { implicit session =>
       val userID = sql"SELECT USER_ID from USER_DEVICES where DEVICE_ID = (SELECT DEVICE_ID FROM DEVICES WHERE DEVICE_UID = ${filterRequest.deviceID}) ORDER BY USER_DEVICE_ID desc LIMIT 1"
        .map(rs => { rs.int("USER_ID") })
        .single
        .apply().get
       val cleaningJobSettingsID = sql"SELECT CLEANING_JOB_SETTINGS_ID from CLEANING_JOB_SETTINGS WHERE CLEANING_JOB_DATA_ID = ${filterRequest.cleaningJobDataID} AND NAME = ${filterRequest.name} ORDER BY CLEANING_JOB_SETTINGS_ID desc LIMIT 1"
        .map(rs => { rs.int("CLEANING_JOB_SETTINGS_ID") })
        .single
        .apply().get
       sql"DELETE FROM CLEANING_JOB_SETTINGS WHERE CLEANING_JOB_DATA_ID = ${filterRequest.cleaningJobDataID} AND NAME = ${filterRequest.name}" // don't worry, prevents SQL injection
        .update 
        .apply() 
       
       sql"DELETE FROM USER_CLEANING_JOB_SETTINGS WHERE USER_ID = ${userID} AND CLEANING_JOB_SETTING_ID = ${cleaningJobSettingsID}" // don't worry, prevents SQL injection
        .update
        .apply()   
     }
  }
  
    
  def getCleaningJobSettings(deviceID:String, cleaningJobID:String): List[CleaningJobSettingContainer] = {
    getSavedDeviceLocation(deviceID)
    NamedDB("mimir_community_server") autoCommit  { implicit session =>
      sql"SELECT cjs.TYPE, cjs.NAME, cjs.SETTING from CLEANING_JOB_DATA cjd join CLEANING_JOB_SETTINGS cjs on cjs.CLEANING_JOB_DATA_ID = cjd.CLEANING_JOB_DATA_ID join USER_CLEANING_JOB_SETTINGS ucjs on ucjs.CLEANING_JOB_SETTING_ID = cjs.CLEANING_JOB_SETTINGS_ID join USER_DEVICES ud on ud.USER_ID = ucjs.USER_ID join DEVICES d on d.DEVICE_ID = ud.DEVICE_ID WHERE d.DEVICE_UID = ${deviceID} and cjd.CLEANING_JOB_ID = ${cleaningJobID}"
        .map(rs => { CleaningJobSettingContainer(rs.string("NAME"), rs.string("TYPE"), rs.string("SETTING")) })
        .list
        .apply()
    }
  }
  
  def getCleaningJobSettingsOptions(cleaningJobID:String): List[SettingOptionContainer] = {
    NamedDB("mimir_community_server") autoCommit  { implicit session =>
      sql"SELECT cjso.TYPE, cjso.NAME, cjso.ID, cjso.OPTION FROM CLEANING_JOB_DATA cjd join CLEANING_JOB_SETTINGS_OPTIONS cjso on cjso.CLEANING_JOB_DATA_ID = cjd.CLEANING_JOB_DATA_ID WHERE cjd.CLEANING_JOB_ID = ${cleaningJobID}"
        .map(rs => { SettingOptionContainer(rs.string("NAME"), rs.string("ID"), rs.string("TYPE"), rs.string("OPTION")) })
        .list
        .apply()
    }
  }
  
  def setDeviceLocation(deviceID:String, lat:Double, lon:Double) : Unit = {
    NamedDB("mimir_community_server") autoCommit  { implicit session =>
           sql"UPDATE DEVICES SET LATITUDE = $lat, LONGITUDE = $lon WHERE DEVICE_UID = ${deviceID}" // don't worry, prevents SQL injection
            .execute                
            .apply() 
           }
    MimirCommunityServer.setLocationMimir(lat, lon)
  }
  
  def getSavedDeviceLocation(deviceID:String) : (Double, Double) = {
    val savedLocation = NamedDB("mimir_community_server") autoCommit  { implicit session =>
           sql"SELECT LATITUDE, LONGITUDE FROM DEVICES WHERE DEVICE_UID = ${deviceID}" // don't worry, prevents SQL injection
            .map(rs => { (rs.double("LATITUDE"), rs.double("LONGITUDE")) })
            .single                
            .apply().get 
           }
    MimirCommunityServer.setLocationMimir(savedLocation._1, savedLocation._2)
    savedLocation
  }
  
  def list(path: String) = {
    val (dir, last) = path.splitAt(path.lastIndexOf("/") + 1)
    val files =
      Option(new java.io.File("./" + dir).listFiles())
        .toSeq.flatten
    for{
      f <- files
      if f.getName.startsWith(last)
    } yield (f.getName, f.length())
  }
}

object MimirCommunityServer {
  def main(args: Array[String]) : Unit = {
    DatabaseManager.connectDatabases()
     
    runWebService()
    
  }
  
  def runWebService() : Unit = {
    startSSLServer(new MainServlet())
    //startServer(new MainServlet())
  }
  
  
  def startServer(theServlet: HttpServlet) {
    val server = new Server(8080)
    
    val http_config = new HttpConfiguration();
    server.addConnector(new ServerConnector( server,  new HttpConnectionFactory(http_config)) );
    
    val resource_handler = new ResourceHandler()
		resource_handler.setDirectoriesListed(true)
		resource_handler.setResourceBase("./app/js/resources")
		val contextHandler = new ContextHandler("/app");
    contextHandler.setResourceBase("./app/js/resources");
    contextHandler.setHandler(resource_handler);
    val resource_handler2 = new ResourceHandler()
		resource_handler2.setDirectoriesListed(true)
		resource_handler2.setResourceBase("./app/js/target/scala-2.11")
    val contextHandler2 = new ContextHandler("/js");
    contextHandler2.setResourceBase("./app/js/target/scala-2.11");
    contextHandler2.setHandler(resource_handler2);
     
    val servletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
    servletContextHandler.setContextPath("/");
    val holder = new ServletHolder(theServlet);
    servletContextHandler.addServlet(holder, "/*");
    
    val handlerList = new HandlerCollection();
    handlerList.setHandlers( Array[Handler](contextHandler, contextHandler2, servletContextHandler, new DefaultHandler()));
    
    server.setHandler(handlerList);
    server.start()
    
     mimirThread.start()
  }
  
  def startSSLServer(theServlet: HttpServlet){
    val contextFactory = new SslContextFactory( );
    contextFactory.setKeyStorePath("websoc.jks");
    contextFactory.setKeyStorePassword("emsys01");
    contextFactory.setKeyManagerPassword("emsys01");
    contextFactory.setKeyStoreType("JKS");
    //contextFactory.setTrustStorePath("websoc_ts.jks");
    //contextFactory.setTrustStorePassword("emsys01");
    //contextFactory.setTrustStoreType("JKS");
    //contextFactory.setNeedClientAuth(true);
    
    val server = new Server();
    val http_config = new HttpConfiguration();
    http_config.setSecureScheme("https");
    http_config.setSecurePort(8089);
    http_config.setOutputBufferSize(32768);
    val https_config = new HttpConfiguration(http_config);
    val src = new SecureRequestCustomizer();
    src.setStsMaxAge(2000);
    src.setStsIncludeSubDomains(true);
    https_config.addCustomizer(src);
    
    val https = new ServerConnector(server, new SslConnectionFactory(contextFactory,HttpVersion.HTTP_1_1.asString()), new HttpConnectionFactory(https_config));
    https.setPort(8089);
    https.setIdleTimeout(500000);
    
    /*val resource_handler = new ResourceHandler()
		resource_handler.setDirectoriesListed(true)
		resource_handler.setResourceBase("./")
		val contextHandler = new ContextHandler("/");
    contextHandler.setResourceBase("./");
    contextHandler.setHandler(resource_handler);
     
    val servletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
    servletContextHandler.setContextPath("/ajax");
    val holder = new ServletHolder(theServlet);
    servletContextHandler.addServlet(holder, "/*");*/*/
    
    val resource_handler = new ResourceHandler()
		resource_handler.setDirectoriesListed(true)
		resource_handler.setResourceBase("./app/js/resources")
		val contextHandler = new ContextHandler("/app");
    contextHandler.setResourceBase("./app/js/resources");
    contextHandler.setHandler(resource_handler);
    val resource_handler2 = new ResourceHandler()
		resource_handler2.setDirectoriesListed(true)
		resource_handler2.setResourceBase("./app/js/target/scala-2.11")
    val contextHandler2 = new ContextHandler("/js");
    contextHandler2.setResourceBase("./app/js/target/scala-2.11");
    contextHandler2.setHandler(resource_handler2);
     
    val servletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
    servletContextHandler.setContextPath("/");
    val holder = new ServletHolder(theServlet);
    servletContextHandler.addServlet(holder, "/*");
    
    val handlerList = new HandlerCollection();
    handlerList.setHandlers( Array[Handler](contextHandler, contextHandler2, servletContextHandler, new DefaultHandler()));
    
    server.addConnector(https);
    server.setHandler(handlerList);
    server.start()
    
    mimirThread.start()
    
    //println(explainCell("SELECT * FROM LENS_MISSING_VALUE1914014057", 1, "3"))
    
  }
  
  def explainCell(query:String, col:Int, row:String): Seq[String] = {
    val reasons = mimirThread.runOnThread[Seq[mimir.ctables.Reason]](new MimirRunnable[Seq[mimir.ctables.Reason] ](){
      def run() = {
        returnedValue = Some(MimirVizier.explainCell(query, col, row))
      }
    })
    
    reasons match {
      case None => Seq()
      case Some(ret:Seq[mimir.ctables.Reason]) => ret.map(reason => reason.toJSON)
    }
  }
  
  def explainEverything(query:String): Seq[mimir.ctables.ReasonSet] = {
    val reasons = mimirThread.runOnThread[Seq[mimir.ctables.ReasonSet]](new MimirRunnable[Seq[mimir.ctables.ReasonSet] ](){
      def run() = {
        returnedValue = Some(MimirVizier.explainEverything(query))
      }
    })
    reasons match {
      case None => Seq()
      case Some(ret:Seq[mimir.ctables.ReasonSet]) => ret
    }
  }
  
  def explainSubset(query:String, rows:Seq[String], cols:Seq[String]): Seq[mimir.ctables.ReasonSet] = {
    val reasons = mimirThread.runOnThread[Seq[mimir.ctables.ReasonSet]](new MimirRunnable[Seq[mimir.ctables.ReasonSet] ](){
      def run() = {
        returnedValue = Some(MimirVizier.explainSubset(query, rows, cols))
      }
    })
    reasons match {
      case None => Seq()
      case Some(ret:Seq[mimir.ctables.ReasonSet]) => ret
    }
  }
  
  def feedback( model:String, idx:Int, args:Seq[String], repairValue:String): Unit = {
    val reasons = mimirThread.runOnThread[Boolean](new MimirRunnable[Boolean](){
      def run() = {
        val argString = 
          if(!args.isEmpty){
            " (" + args.mkString(",") + ")"
          } else { "" }
        MimirVizier.db.update(MimirVizier.db.parse(s"FEEDBACK ${model} ${idx}$argString IS ${ repairValue }").head)
        returnedValue = None
      }
    })
    println("")
  }
  
  def setLocationMimir( lat:Double, lon:Double): Unit = {
    val reasons = mimirThread.runOnThread[Boolean](new MimirRunnable[Boolean](){
      def run() = {
        mimir.sql.sqlite.MeToLocationDistance.myLat = Some(lat)
        mimir.sql.sqlite.MeToLocationDistance.myLon = Some(lon) 
        returnedValue = None
      }
    })
    println("")
  }
  
  def queryMimir(query:String): (Vector[String], Vector[CleaningJobDataRow]) = {
    val queryResults = mimirThread.runOnThread[(Vector[String], Vector[CleaningJobDataRow])](new MimirRunnable[(Vector[String], Vector[CleaningJobDataRow])](){
      def run() = {
        val oper = MimirVizier.parseQuery(query)
        val results = new java.util.Vector[Row]()
         var cols : Seq[String] = null
         var colsIndexes : Seq[Int] = null
         
         MimirVizier.db.query(oper)( resIter => {
             cols = resIter.schema.map(f => f._1)
             colsIndexes = resIter.schema.zipWithIndex.map( _._2)
             while(resIter.hasNext())
               results.add(resIter.next)
         })
         val resCSV = results.toArray[Row](Array()).seq.map(row => {
           val truples = colsIndexes.map( (i) => {
             CleaningJobDataCell(row(i).toString, row.isColDeterministic(i))  
           })
           CleaningJobDataRow(truples.toVector, row.isDeterministic(), row.provenance.asString)
         })
         returnedValue = Some((cols.toVector, resCSV.toVector))
      }
    })
    queryResults match {
      case None => ( Vector[String](),  Vector[CleaningJobDataRow]())
      case Some(ret:(Vector[String], Vector[CleaningJobDataRow])) => ret
    }
  }
  
  abstract class MimirRunnable[ReturnType] extends Runnable {
    protected var returnedValue : Option[ReturnType] = None;
    def getReturnedValue() : Option[ReturnType] = {
      returnedValue
    }
  }
  
  class MimirThread extends Thread {
    val execQueue = new LinkedBlockingQueue[MimirRunnable[_]]()
    val returnQueue = new LinkedBlockingQueue[Any]()
    override def run() = {
      MimirVizier.main(Array("--db","mimir_community_clean.db","--X","INLINE-VG","GPROM-PROVENANCE", "NO-VISTRAILS"))
      while(true){
        val runnable = execQueue.take()
        runnable.run()
        returnQueue.put( runnable.getReturnedValue )
      }
    }
    def runOnThread[ReturnType](runnable:MimirRunnable[ReturnType]) : Option[ReturnType] = {
      synchronized {
        execQueue.put(runnable)
        returnQueue.take().asInstanceOf[Option[ReturnType]]
      }
    }
  }
  
  val mimirThread = new MimirThread()
}