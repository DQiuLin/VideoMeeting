/**
  * User: gaohan
  * Date: 2019/10/28
  * Time: 14:58
  */
package org.seekloud.theia.processor

import java.io.{File, FileInputStream, InputStream, OutputStream}
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.Pipe.{SinkChannel, SourceChannel}
import java.nio.channels.{Channels, DatagramChannel}
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ExecutorService, Executors}

import videomeeting.processor.Boot.executor
import videomeeting.processor.utils.HttpUtil
import videomeeting.processor.utils.ProcessorClient.{CloseRoom, SuccessRsp, UpdateRoom}
import videomeeting.protocol.ptcl.processer2Manager.Processor.{NewConnect, NewConnectRsp}
import org.slf4j.LoggerFactory
import videomeeting.processor.stream.PipeStream
import videomeeting.processor.utils.HttpUtil

import scala.concurrent.Future

object TestThread extends HttpUtil{

  import io.circe.generic.auto._
  import io.circe.parser.decode
  import io.circe.syntax._

  val maxTsNum = 7
  var ts = 0l

  object Rtp_header{
    var m = 0
    var timestamp = 0l
    var payloadType = 96.toByte //负载类型号96
  }

  private val log = LoggerFactory.getLogger(this.getClass)
 // val processorBaseUrl = "http://10.1.29.248:30388/theia/processor"
  val processorBaseUrl = "http://127.0.0.1:30388/theia/processor"

//  def test1(n:Int) = {
//    val num = n
//    run(num)
//    Thread.sleep(120*1000)
//    for(i <- 1000 until 1000+num) {
//      closeRoom(i).map{a =>
//        println(a)
//      }
//      Thread.sleep(50)
//    }
//
//  }

//  def test3() = {
//    val dst = new InetSocketAddress("127.0.0.1", 41100)
//    val udpSender = DatagramChannel.open()
//    val src = "C:\\Users\\yuwei\\Desktop\\record.ts"
//    val fis = new FileInputStream(new File(src))
//    while (true){
//      val buf_tempRead = new Array[Byte](188*7)
//      fis.read(buf_tempRead)
//      udpSender.send(ByteBuffer.wrap(buf_tempRead),dst)
//    }
//  }

  def udpReceiver(buf: ByteBuffer,out1: SinkChannel, out2: SinkChannel ) = {

    val channel = DatagramChannel.open()
    channel.socket().bind(new InetSocketAddress("127.0.0.1", 41100))
    // while(true) {
    buf.clear()
    val buf_temp= new Array[Byte](188)

    channel.receive(buf)
    buf.get(buf_temp)
    buf.flip()
    out1.write(buf)
    out2.write(buf)
    // }

  }


  def main(args: Array[String]): Unit = {

    val threadPool:ExecutorService=Executors.newFixedThreadPool(60)

    val pipe1 = new PipeStream
    val source1 = pipe1.getSource
    val sink1 = pipe1. getSink
    val out1 = Channels.newOutputStream(sink1)
    val in1 = Channels.newInputStream(source1)

    val pipe2 = new PipeStream
    val source2 = pipe2.getSource
    val sink2 = pipe2. getSink
    val out2 = Channels.newOutputStream(sink2)
    val in2 = Channels.newInputStream(source2)

    val buf = ByteBuffer.allocate(1024 * 32)

    udpReceiver(buf,sink1, sink2 )
    println(buf)

    try {
      var ssrc4Host = 0
      var ssrc4Client = 1
      var ssrc4Push = 2
      for(i <- 1000 until 1000+1){
        ssrc4Client += 1
        ssrc4Host += 1
        ssrc4Push += 1
        newConnect(i.toLong, s"$ssrc4Host",s"$ssrc4Client", s"$ssrc4Push", "", 1)
        Thread.sleep(3000)
        threadPool.execute(new ThreadTest(ssrc4Host, source1))
        threadPool.execute(new ThreadTest(ssrc4Client, source2))
      }
    }finally {
      threadPool.shutdown()
    }


  }


  def newConnect(roomId: Long, host: String, client: String, pushLiveId: String, liveCode: String, layout: Int): Future[Either[String, NewConnectRsp]] = {
    val url = processorBaseUrl + "/newConnect"
    val jsonString = NewConnect(roomId,host,client,pushLiveId,liveCode, layout).asJson.noSpaces
    postJsonRequestSend("post",url,List(),jsonString,timeOut = 60 * 1000,needLogRsp = false).map{
      case Right(v) =>
        decode[NewConnectRsp](v) match {
          case Right(data) =>
            Right(data)
          case Left(e) =>
            log.error(s"connectRoom error :$e")
            Left("error")
        }
      case Left(error) =>
        log.error(s"connectRoom postJsonRequestSend error : $error")
        Left("Error")
    }
  }
  def updateRoomInfo(roomId:Long,liveIdList:List[String],startTime:Long,layout:Int,aiMode:Int):Future[Either[String,SuccessRsp]] = {
    val url = processorBaseUrl + "/updateRoomInfo"
    val jsonString = UpdateRoom(roomId,liveIdList,startTime,layout,aiMode).asJson.noSpaces
    //    val jsonData = genPostEnvelope("",System.nanoTime().toString,jsonString,"").asJson.noSpaces
    postJsonRequestSend("post",url,List(),jsonString,timeOut = 60 * 1000,needLogRsp = false).map{
      case Right(v) =>
        decode[SuccessRsp](v) match{
          case Right(data) =>
            Right(data)
          case Left(e) =>
            log.error(s"updateRoomInfo decode error : $e")
            Left("Error")
        }
      case Left(error) =>
        log.error(s"updateRoomInfo postJsonRequestSend error : $error")
        Left("Error")
    }
  }

  def closeRoom(roomId:Long):Future[Either[String,SuccessRsp]] = {
    val url = processorBaseUrl + "/closeRoom"
    val jsonString = CloseRoom(roomId).asJson.noSpaces
    postJsonRequestSend("post",url,List(),jsonString,timeOut = 60 * 1000).map{
      case Right(v) =>
        decode[SuccessRsp](v) match{
          case Right(data) =>
            Right(data)
          case Left(e) =>
            log.error(s"updateRoomInfo decode error : $e")
            Left("Error")
        }
      case Left(error) =>
        log.error(s"updateRoomInfo postJsonRequestSend error : $error")
        Left("Error")
    }
  }


  class ThreadTest(ssrc:Int, fis: SourceChannel ) extends Runnable{
    override def run(){
      println( "ssrc",ssrc)
      val port = 30388
              val host = "127.0.0.1"
      //val host = "10.1.29.248"
      val increasedSequence = new AtomicInteger(0)
      val frameRate = 25
      val timestamp_increse=90000/frameRate//framerate是帧率
      val tsBuf1 = (0 until maxTsNum).map {i =>
        ByteBuffer.allocate(188 * 1)
      }.toList
      var count = 0

      //setup sink
      val dst = new InetSocketAddress(host, port)
      val udpSender = DatagramChannel.open()


      //从pipe中读取

      var countRead = 0
      var totalReadSize = 0
      val buf = ByteBuffer.allocate(1024 * 32)
      var buf_temp =  Array[Byte]()
      buf.clear()
      val len = fis.read(buf)
      while(len > 0){
        countRead += 1
        totalReadSize += len.toInt
        buf.flip()
        Thread.sleep(2)
        //          println(s"thread = $threadCount read count=$countRead, len=$len totalSize=$totalReadSize")

        buf_temp = (buf_temp.toList ::: buf.array().take(buf.remaining()).toList).toArray
        println(buf_temp.length)
        while(buf_temp.length >= 188 *2) {
          var first_flag = true
          while(first_flag && buf_temp.length >= 188 * 2) {
            first_flag = false
            if (buf_temp(0) != 0x47) { //drop掉188字节以外的数据
              var ifFindFlag = -1
              var i = 0
              for(a <- buf_temp if ifFindFlag == -1) {
                if (a == 0x47.toByte) {ifFindFlag = i; buf_temp = buf_temp.drop(i)}
                i += 1
              }
            }
            println("++++++++++++++")
            while (count < 7 && !first_flag && buf_temp.length >= 188){
              val ts_packet = buf_temp.take(188)
              buf_temp = buf_temp.drop(188)
              if (ts_packet(0) != 0x47) {
                println("===========================error========================")
              }
              else {
                tsBuf1(count).put(ts_packet)
                tsBuf1(count).flip()
                count += 1
                if ((ts_packet(1) | 191.toByte).toByte == 255.toByte) {
                  val total_len = 12 + count * 188
                  val rtp_buf = ByteBuffer.allocate(total_len)
                  Rtp_header.m = 1
                  Rtp_header.timestamp += timestamp_increse //到下一个起始帧或者满了7个包，填充完毕
                  first_flag = true
                  //设置rtp header

                  //设置序列号
                  val seq = increasedSequence.getAndIncrement()
                  println(s"seq", seq)
                  rtp_buf.put(0x80.toByte)
                  rtp_buf.put(33.toByte)
                  toByte(seq, 2).map(rtp_buf.put(_))
                  toByte(System.currentTimeMillis().toInt, 4).map(rtp_buf.put(_))
                  rtp_buf.putInt(ssrc)
                  //                    println(s"threadCount = $threadCount, seq", seq,"ssrc",ssrc)
                  (0 until count).foreach(i => rtp_buf.put(tsBuf1(i)))

                  //                println(s"-----------------------")

                  rtp_buf.flip()

                  udpSender.send(rtp_buf, dst) //此rtp包是最后一个包
                  //                println(s"send")

                  (0 until count).foreach{i =>
                    tsBuf1(i).clear()}
                  rtp_buf.clear()
                  count = 0
                  println("77777777777777")
                } else
                if (count == 7) {
                  val total_len = 12 + count * 188
                  val rtp_buf = ByteBuffer.allocate(total_len)
                  //                println("满7片，send rtp包")
                  Rtp_header.m = 0

                  //设置序列号
                  val seq = increasedSequence.getAndIncrement()
                  rtp_buf.put(0x80.toByte)
                  rtp_buf.put(33.toByte)
                  toByte(seq, 2).map(rtp_buf.put(_))
                  toByte(System.currentTimeMillis().toInt, 4).map(rtp_buf.put(_))
                  rtp_buf.putInt(ssrc)
                  //                    println(s"threadCount = $threadCount, seq", seq,"ssrc",ssrc)
                  (0 until count).foreach(i => rtp_buf.put(tsBuf1(i)))
                  //                println(s"-----------------------")

                  rtp_buf.flip()
                  udpSender.send(rtp_buf, dst)
                  //                println(s"send")
                  (0 until count).foreach{i =>
                    tsBuf1(i).clear()}
                  rtp_buf.clear()
                  count = 0
                  println("6666666666666")

                }
              }
            }
          }
        }
      }
    }
  }

  def toByte(num: Long, byte_num: Int) = {
    (0 until byte_num).map { index =>
      (num >> ((byte_num - index - 1) * 8) & 0xFF).toByte
    }.toList
  }
}
