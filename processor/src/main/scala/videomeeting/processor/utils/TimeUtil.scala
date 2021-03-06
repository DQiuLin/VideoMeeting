package videomeeting.processor.utils

import java.sql.Date
import java.text.SimpleDateFormat
import java.util.{Calendar, Locale}

import com.github.nscala_time.time.StaticInterval


/**
  * Created by wangchunze on 2016/3/23.
  */
object TimeUtil {

  def format(timeMs:Long,format:String = "yyyy-MM-dd HH:mm:ss.SSS") ={
    val data  = new Date(timeMs)
    val simpleDateFormat = new SimpleDateFormat(format)
    simpleDateFormat.format(data)
  }

  def getMinuteOfNow={
    val data  = new Date(System.currentTimeMillis())
    val format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")
    format.format(data).split("-")(4).toInt
  }

  /**
    * 格式化时间 输入时某天开始的分钟数
    * @param minute
    */
  def formatFromMinute(minute:Long)={
    import com.github.nscala_time.time.Imports._
    val triggerTime = DateTime.now.hour(0).minute(0).second(0).getMillis
    format(triggerTime+minute*60*1000,"HH:mm:ss")
  }

  /**
    * 日期转时间戳
    * @param date 格式：20160518
    */
  def parseDate(date:String)={
    val year=date.take(4).toInt
    val month=date.slice(4,6)
    val day=date.takeRight(2)
    new SimpleDateFormat("yyyy-MM-dd")
      .parse(year+"-"+month+"-"+day)
      .getTime
  }


  /**
    * 获取之前某天的日期  返回格式 20160518
    */
  def getDateDaysBefore(timeMs:Long,n:Int)={
    val data  = new Date(timeMs-n*3600*24*1000)
    val simpleDateFormat = new SimpleDateFormat("yyyyMMdd")
    simpleDateFormat.format(data)
  }

  /**
    * 获取日期 返回格式 20160519
    * @param date 格式 20160518
    * @param n  n=1 表示前一天
    * @return
    */
  def getDateBeforeNow(date:String,n:Int)={
    val now=parseDate(date)
    getDateDaysBefore(now,n)
  }

  /**
    * 获取一个月的开始时间戳
    * @param now
    */
  def getMonthStart(now:Long)={
    val cal = Calendar.getInstance()
    cal.setTime(new Date(now))
    cal.set(Calendar.DATE,1)
    val df = new SimpleDateFormat("yyyyMMdd")
    parseDate(df.format(cal.getTime)) //本月第一天
  }

  def getMonthEnd(now:Long)={
    val cal =Calendar.getInstance()
    cal.setTime(new Date(now))
    val df = new SimpleDateFormat("yyyyMMdd")
    cal.add(Calendar.MONTH,1)
    cal.set(Calendar.DATE, 1)
    parseDate(df.format(cal.getTime))//本月最后一天
  }


  def getBeginOfDay(now:Long)={
    import com.github.nscala_time.time.Imports._
    DateTime.now.hour(0).minute(0).second(0).getMillis
  }

  def getBeginOfDayOfSec(now:Long)={
    import com.github.nscala_time.time.Imports._
    DateTime.now.hour(0).minute(0).second(0).getMillis / 1000
  }

  def getLastWeek={
    (StaticInterval.lastWeek().getStartMillis,StaticInterval.lastWeek().getEndMillis)
  }

  def getDay(now:Long)={
    val data  = new Date(now)
    val format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")
    format.format(data).split("-")(2).toInt
  }

  def getLastMonth={
    (StaticInterval.lastMonth().getStartMillis,StaticInterval.lastMonth().getEndMillis)
  }

  def getLastDay={
    (StaticInterval.lastDay().getStartMillis,StaticInterval.lastDay().getEndMillis)
  }

  /**
    * 日期转时间戳
    * @param date 格式：2016-09-29
    */
  def date2TimeStamp(date:String,f:String = "yyyy-MM-dd HH:mm:ss")={
    new SimpleDateFormat(f).parse(date).getTime
  }

  def date2TimeStamp2(date:String) = {
    try {
      val sdf = new SimpleDateFormat("E MMM dd HH:mm:ss yyyy",Locale.ENGLISH)
      sdf.parse(date).getTime
    }catch{
      case e:Exception=>
        println(e)
        0l
    }
  }

  def conveyDateTime(data:String,f1:String="E MMM dd HH:mm:ss yyyy",f2:String="yyyy-MM-dd HH:mm:ss") = {
    try {
      val sdf = new SimpleDateFormat(f1,Locale.ENGLISH)
      val simpleDateFormat = new SimpleDateFormat(f2)
      simpleDateFormat.format(sdf.parse(data))
    }catch{
      case e:Exception=>
        println(e)
        data
    }
  }

  def getSimpleDataTime(t:Long) = {
    import com.github.nscala_time.time.Imports._
    val start = DateTime.now.hour(0).minute(0).second(0).getMillis
    try {
      if (t > start) {
        val f = new SimpleDateFormat("HH:mm:ss")
        f.format(new Date(t))
      } else if(t>0l) {
        val f = new SimpleDateFormat("yyyy-MM-dd")
        f.format(new Date(t))
      }else{
        ""
      }
    }catch{
      case e:Exception=>
        ""
    }
  }

  def getEnglishTime(timeMs:Long,format:String = "E MMM dd HH:mm:ss yyyy") ={
    val data  = new Date(timeMs)
    val sdf = new SimpleDateFormat(format,Locale.ENGLISH)
    sdf.format(data)
  }



  def main(args: Array[String]) {
    println(getSimpleDataTime(1509093763252l))
    println(format(System.currentTimeMillis()))
  }

}
