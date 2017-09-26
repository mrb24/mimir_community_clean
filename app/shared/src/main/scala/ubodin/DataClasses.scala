package ubodin
  
case class CleaningJob(cleaningJobID: Int, name: String, jobType:String, imagePath: String, tags: Stream[String]) 

case class UserScheduleItem (
  userID: Int,
  cleaningJob: CleaningJob,  
  scheduleCleaningJobsID: Int,
  startTime: Double,
  endTime: Double,
  startedTime: Double,
  endedTime: Double
)

case class UserSchedule(
  scheduleItems: Array[UserScheduleItem]    
)

case class UserInfoData(
  userID : Int,
  firstName: String,
  lastName: String,
  deviceIDs: Array[String],
  userSchedule: UserSchedule
  )
  
case class CleaningJobInfo(cleaningJobDataID: Int, cleaningJobID: Int, name: String, query: String) 

case class CleaningJobDataCell(var data:String, var isDet: Boolean) 

case class CleaningJobDataRow(data:Vector[CleaningJobDataCell], isDet: Boolean, prov: String) 

case class CleaningJobData(cleaningJobDataID: Int, cleaningJobID: Int, cols:Vector[String], data:Vector[CleaningJobDataRow]) 

///--------------------------------------------
/// Reasons and Repairs
///--------------------------------------------
abstract class CleaningJobTaskRepair(val repairType:String)

object CleaningJobTaskRepair {
  def readRepair(repairType:String, repairJson:String) : CleaningJobTaskRepair = {
    repairType match {
      case "RepairByType" => upickle.read[CleaningJobTaskRepairByType](repairJson)
      case "RepairFromList" => upickle.read[CleaningJobTaskRepairFromList](repairJson)
      case "ModerationRepair" => upickle.read[CleaningJobTaskModerationRepair](repairJson)
      
    }
  }
  def writeRepair(repair:CleaningJobTaskRepair) : String = {
    repair match {
      case repairByType@CleaningJobTaskRepairByType(_,_) => upickle.write(repairByType)
      case repairFromList@CleaningJobTaskRepairFromList(_,_) => upickle.write(repairFromList)
      case moderationRepair@CleaningJobTaskModerationRepair(_) => upickle.write(moderationRepair)
      
    }
  }
  def containRepair(repairType:String, repair:CleaningJobTaskRepair) : CleaningJobRepairContainer = {
    CleaningJobRepairContainer(repairType, writeRepair(repair))
  }
  def decontainRepair(containedRepair: CleaningJobRepairContainer) : CleaningJobTaskRepair = {
    readRepair(containedRepair.repairType, containedRepair.repairJson)
  }
}

case class CleaningJobRepairContainer(repairType:String, repairJson:String)

case class CleaningJobTaskRepairByType(
  selector: String,
  `type`: String
) extends CleaningJobTaskRepair("RepairByType")

case class CleaningJobTaskRepairFromListValues(
  choice: String,
  weight: Double
)

case class CleaningJobTaskRepairFromList(
  selector: String,
  values: List[CleaningJobTaskRepairFromListValues]
) extends CleaningJobTaskRepair("RepairFromList")

case class CleaningJobTaskModerationRepair( value: String ) 
  extends CleaningJobTaskRepair("ModerationRepair")

case class CleaningJobTask(
  source: String,
  varid: Double,
  english: String,
  repair: CleaningJobRepairContainer,
  args: List[String]//,
  //hints: List[String]
)

case class CleaningJobTaskGroup( tasks:List[CleaningJobTask] )

///----------------------------------------
/// Settings
///----------------------------------------
abstract class CleaningJobSetting(val settingType:String) 

object CleaningJobSetting {
  def readSetting(settingType:String, settingJson:String) : CleaningJobSetting = {
    settingType match {
      case "FILTER" => upickle.read[CleaningJobFilterSetting](settingJson)
      case "CLUSTER" => upickle.read[CleaningJobMapClustererSetting](settingJson)
      
    }
  }
  def writeSetting(setting:CleaningJobSetting) : String = {
    setting match {
      case filterSetting@CleaningJobFilterSetting(cleaningJobSettingID, cleaningJobDataIDt, queryWhere) => upickle.write(filterSetting)
      case clusterSteeing@CleaningJobMapClustererSetting() => upickle.write(clusterSteeing)
      
    }
  }
  def containSetting(name:String, settingType:String, setting:CleaningJobSetting) : CleaningJobSettingContainer = {
    CleaningJobSettingContainer(name, settingType, writeSetting(setting))
  }
  def decontainSetting(containedSetting: CleaningJobSettingContainer) : CleaningJobSetting = {
    readSetting(containedSetting.settingType, containedSetting.settingJson)
  }
}

case class CleaningJobFilterSetting(cleaningJobSettingID: Int, cleaningJobDataID: Int, queryWhere:String) extends CleaningJobSetting("FILTER") 

case class CleaningJobMapClustererSetting() extends CleaningJobSetting("CLUSTER")

case class CleaningJobSettingContainer(name:String, settingType:String, settingJson:String)


///--------------------------------------------
/// Setting Options
///--------------------------------------------
abstract class SettingOption(val optionType:String) 

object SettingOption {
  def readOption(optionType:String, optionJson:String) : Option[SettingOption] = {
    optionType match {
      case "LOCATION_FILTER" => Some(upickle.read[LocationFilterSettingOption](optionJson))
      case "MAP_CLUSTERER" => Some(upickle.read[MapClustererSettingOption](optionJson))
      case "GIS_LAT_LON_COLS" => Some(upickle.read[GISLatLonColsSettingOption](optionJson))
      case "GIS_ADDR_COLS" => Some(upickle.read[GISAddressColsSettingOption](optionJson))
      case _ => None
    }
  }
  def writeOption(option:Option[SettingOption]) : String = {
    option match {
      case Some(locationFilterOption@LocationFilterSettingOption(distance, latCol, lonCol)) => upickle.write(locationFilterOption)
      case Some(mapClusterer@MapClustererSettingOption()) => upickle.write(mapClusterer)
      case Some(latLonCols@GISLatLonColsSettingOption(latCol, lonCol)) => upickle.write(latLonCols)
      case Some(addrCols@GISAddressColsSettingOption(_,_,_,_)) => upickle.write(addrCols)
      case None => ""
    }
  }
  def containOption(name:String, id:String, optionType:String, option:Option[SettingOption]) : SettingOptionContainer = {
    SettingOptionContainer(name, id, optionType, writeOption(option))
  }
  def decontainOption(containedOption: SettingOptionContainer) : Option[SettingOption] = {
    readOption(containedOption.optionType, containedOption.optionJson)
  }
}

case class LocationFilterSettingOption(distance:Double, latCol:String, lonCol:String) extends SettingOption("LOCATION_FILTER") 

case class MapClustererSettingOption() extends SettingOption("MAP_CLUSTERER") 

case class GISLatLonColsSettingOption(latCol:String, lonCol:String) extends SettingOption("GIS_LAT_LON_COLS") 

case class GISAddressColsSettingOption(houseNumber:String, street:String, city:String, state:String) extends SettingOption("GIS_ADDR_COLS") 

case class SettingOptionContainer(name:String, id:String, optionType:String, optionJson:String)


