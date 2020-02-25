import java.io.File
import java.util
import java.util.concurrent.TimeUnit

import org.bytedeco.javacpp.Loader

object TestCmd {

  def main(args: Array[String]): Unit = {

    val ffmpeg = Loader.load(classOf[org.bytedeco.ffmpeg.ffmpeg])
    print(ffmpeg)
//    val pb = new ProcessBuilder(ffmpeg, "-i", s"udp://127.0.0.1:41100", "-f", "hls", "-hls_time", "10", "-segment_list_flags", "+live","C:\\Users\\yuwei\\Desktop\\test\\test.m3u8")
//    val pb = new ProcessBuilder(ffmpeg, "-re", "-i", "/Users/litianyu/Downloads/test.mp4", "-c:v", "libx264", "-s", "720x576",
//      "-c:a", "copy", "-f", "hls","-b:v","1M", "-hls_time", "3", "-segment_list_flags", "+live","-hls_list_size", "20","-vcodec", "copy", "-acodec", "copy",
//      "/Users/litianyu/Downloads/test2/test.m3u8")
    val commandStr = ffmpeg + " -i \"/home/sk75/distributor/test/video.mp4\" -i \"/home/sk75/distributor/test/audio.mp4\" -c:v copy -c:a copy /home/sk75/distributor/test/final.mp4"
    val cmd: util.ArrayList[String] = new util.ArrayList[String]()
    commandStr.split(" ").foreach(cmd.add)
    val pb: ProcessBuilder = new ProcessBuilder(cmd)
    //    val pb = new ProcessBuilder(ffmpeg, "-i", "D:\\test\\video\\video.mp4", "-b:v", "1M", "-f", "dash", "-window_size", "20", "-extra_window_size", "20", "-hls_playlist", "1", "D:\\qycache\\download\\convert\\test.mpd")
    val processor = pb.inheritIO().start()
    Thread.sleep(60000)
  }
}
