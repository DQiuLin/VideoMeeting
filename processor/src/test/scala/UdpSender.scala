//import java.io.{File, FileInputStream}
//import java.net.InetSocketAddress
//import java.nio.ByteBuffer
//import java.nio.channels.{Channels, DatagramChannel, Pipe}
//import java.util.concurrent.atomic.AtomicInteger
//
//import TestThread.{Rtp_header, toByte}
//import org.bytedeco.javacv.{FFmpegFrameGrabber, Frame}
//
///**
//  * Created by sky
//  * Date on 2019/10/10
//  * Time at 下午4:08
//  */
//
//
//
//object UdpSender {
//
//  def main(args: Array[String]): Unit = {
//    new UdpSender(10).run()
//    Thread.sleep(1000000)
//  }
//}
//
//
//class UdpSender(ssrc:Int) extends Runnable{
//  override def run(){
//    println( "ssrc",ssrc)
//    val port = 41100
//    //        val host = "127.0.0.1"
//    val host = "10.1.120.133"
//    val increasedSequence = new AtomicInteger(0)
//    val frameRate = 25
//    val timestamp_increse=90000/frameRate//framerate是帧率
//    val tsBuf1 = (0 until 7).map {i =>
//      ByteBuffer.allocate(188 * 1)
//    }.toList
//    var buf_temp = Array[Byte]()
//    var count = 0
//
//    //setup sink
//    val dst = new InetSocketAddress(host, port)
//    val udpSender = DatagramChannel.open()
//
//    //读取视频文件
//    //        val src = "D:/test/111.ts"
//    //        val src = "C:\\Users\\yuwei\\Desktop\\record.ts\u202AC:\\Users\\yuwei\\Desktop\\record.ts"
//    val src = "/Users/sky/IdeaProjects/theia/faceAnalysis/test_1570693569309.ts"
//    val fis = new FileInputStream(new File(src))
//    var countRead = 0
//    var totalReadSize = 0
//    val bufRead = ByteBuffer.allocate(1024 * 32)
//    val buf_tempRead = new Array[Byte](188)
//    bufRead.clear()
//    var len = fis.read(buf_tempRead,0,188)
//
//    while(len != -1){
//      countRead += 1
//      totalReadSize += len.toInt
//      for(i <- buf_tempRead.indices){
//        bufRead.put(buf_tempRead(i))
//      }
//      bufRead.flip()
//      Thread.sleep(2)
//      //          println(s"thread = $threadCount read count=$countRead, len=$len totalSize=$totalReadSize")
//
//      buf_temp = (buf_temp.toList ::: bufRead.array().take(bufRead.remaining()).toList).toArray
//
//      while(buf_temp.length >= 188 *2) {
//        var first_flag = true
//        while(first_flag && buf_temp.length >= 188 * 2) {
//          first_flag = false
//          if (buf_temp(0) != 0x47) { //drop掉188字节以外的数据
//            var ifFindFlag = -1
//            var i = 0
//            for(a <- buf_temp if ifFindFlag == -1) {
//              if (a == 0x47.toByte) {ifFindFlag = i; buf_temp = buf_temp.drop(i)}
//              i += 1
//            }
//
//          }
//          while (count < 7 && !first_flag && buf_temp.length >= 188){
//            val ts_packet = buf_temp.take(188)
//            buf_temp = buf_temp.drop(188)
//            if (ts_packet(0) != 0x47) {
//              println("===========================error========================")
//            }
//            else {
//              tsBuf1(count).put(ts_packet)
//              tsBuf1(count).flip()
//              count += 1
//              if ((ts_packet(1) | 191.toByte).toByte == 255.toByte) {
//                val total_len = 12 + count * 188
//                val rtp_buf = ByteBuffer.allocate(total_len)
//                Rtp_header.m = 1
//                Rtp_header.timestamp += timestamp_increse //到下一个起始帧或者满了7个包，填充完毕
//                first_flag = true
//                //设置rtp header
//
//                //设置序列号
//                val seq = increasedSequence.getAndIncrement()
//
//                rtp_buf.put(0x80.toByte)
//                rtp_buf.put(33.toByte)
//                toByte(seq, 2).map(rtp_buf.put)
//                toByte(System.currentTimeMillis().toInt, 4).map(rtp_buf.put)
//                rtp_buf.putInt(ssrc)
//                //                    println(s"threadCount = $threadCount, seq", seq,"ssrc",ssrc)
//                (0 until count).foreach(i => rtp_buf.put(tsBuf1(i)))
//
//                //                println(s"-----------------------")
//
//                rtp_buf.flip()
//                println(s"send $seq")
//                udpSender.send(rtp_buf, dst) //此rtp包是最后一个包
//                //                println(s"send")
//
//                (0 until count).foreach{i =>
//                  tsBuf1(i).clear()}
//                rtp_buf.clear()
//                count = 0
//              } else
//              if (count == 7) {
//                val total_len = 12 + count * 188
//                val rtp_buf = ByteBuffer.allocate(total_len)
//                //                println("满7片，send rtp包")
//                Rtp_header.m = 0
//
//                //设置序列号
//                val seq = increasedSequence.getAndIncrement()
//                rtp_buf.put(0x80.toByte)
//                rtp_buf.put(33.toByte)
//                toByte(seq, 2).map(rtp_buf.put)
//                toByte(System.currentTimeMillis().toInt, 4).map(rtp_buf.put)
//                rtp_buf.putInt(ssrc)
//                //                    println(s"threadCount = $threadCount, seq", seq,"ssrc",ssrc)
//                (0 until count).foreach(i => rtp_buf.put(tsBuf1(i)))
//                //                println(s"-----------------------")
//
//                rtp_buf.flip()
//                println(s"send $seq")
//                udpSender.send(rtp_buf, dst)
//                //                println(s"send")
//                (0 until count).foreach{i =>
//                  tsBuf1(i).clear()}
//                rtp_buf.clear()
//                count = 0
//              }
//            }
//          }
//        }
//      }
//      bufRead.clear()
//      len = fis.read(buf_tempRead,0,188)
//    }
//  }
//}