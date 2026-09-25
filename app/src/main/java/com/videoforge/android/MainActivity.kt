package com.videoforge.android

import android.app.Activity
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.graphics.*
import android.media.*
import android.view.*
import android.widget.*
import java.io.File
import kotlin.math.min

data class Scene(val title:String,val subtitle:String,val kind:Int,val durationMs:Long=5000)
data class VideoProject(val width:Int=1280,val height:Int=720,val fps:Int=30,val scenes:List<Scene>)

class MainActivity:Activity(){
 private lateinit var editor:VideoCanvasView; private lateinit var status:TextView
 override fun onCreate(b:Bundle?){super.onCreate(b)
  val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.rgb(11,13,15))}
  val head=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(24,12,24,12)}
  val brand=TextView(this).apply{text="VideoForge";textSize=22f;setTextColor(Color.WHITE);setTypeface(Typeface.DEFAULT,Typeface.BOLD)}
  head.addView(brand,LinearLayout.LayoutParams(0,64,1f))
  status=TextView(this).apply{text="Ready";textSize=14f;setTextColor(Color.LTGRAY)}
  head.addView(status,LinearLayout.LayoutParams(220,64));root.addView(head)
  editor=VideoCanvasView(this);root.addView(editor,LinearLayout.LayoutParams(-1,0,1f))
  val bar=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(18,8,18,16)}
  fun btn(s:String)=Button(this).apply{text=s;isAllCaps=false}
  val preview=btn("Preview");val next=btn("Next scene");val export=btn("Export MP4")
  bar.addView(preview,LinearLayout.LayoutParams(0,58,1f));bar.addView(next,LinearLayout.LayoutParams(0,58,1f));bar.addView(export,LinearLayout.LayoutParams(0,58,1f));root.addView(bar)
  preview.setOnClickListener{editor.play();status.text="Previewing"}
  next.setOnClickListener{editor.nextScene();status.text="Scene ${editor.sceneIndex+1}/${editor.sceneCount}"}
  export.setOnClickListener{export.isEnabled=false;status.text="Rendering…";Thread{try{val f=VideoExporter(this).render(editor.project);runOnUiThread{status.text="Saved: ${f.name}";Toast.makeText(this,"Saved to Movies/VideoForge",Toast.LENGTH_LONG).show();export.isEnabled=true}}catch(e:Exception){runOnUiThread{status.text="Export failed: ${e.message}";export.isEnabled=true}}}}.start()}
  setContentView(root)
 }
}

class VideoCanvasView(private val ctx:android.content.Context):View(ctx){
 val project=VideoProject(scenes=listOf(
  Scene("Why does ₹100 buy less?","A simple introduction to inflation.",0),
  Scene("Inflation = prices rising","The general price level rises over time.",1),
  Scene("Your ₹100 basket","The same money buys fewer goods.",2),
  Scene("Why do prices rise?","Demand-pull and cost-push pressures.",3),
  Scene("Measure → understand → respond","CPI tracks the cost of a basket of goods.",4)))
 var sceneIndex=0; val sceneCount get()=project.scenes.size; private var start=SystemClock.uptimeMillis();private var playing=false
 private val text=Paint(1);private val muted=Paint(1);private val accent=Paint(1)
 init{ text.color=Color.WHITE;muted.color=Color.rgb(190,195,205);accent.color=Color.rgb(124,92,255)}
 fun nextScene(){sceneIndex=(sceneIndex+1)%sceneCount;start=SystemClock.uptimeMillis();invalidate()}
 fun play(){playing=true;start=SystemClock.uptimeMillis();invalidate()}
 fun drawFrame(c:Canvas,timeMs:Long){c.drawColor(Color.rgb(11,13,15));val s=project.scenes[sceneIndex];val scale=min(width/1280f,height/720f);c.save();c.scale(scale,scale);val p=((timeMs%s.durationMs).toFloat()/s.durationMs).coerceIn(0f,1f);val e=1-(1-p)*(1-p)
  text.typeface=Typeface.DEFAULT_BOLD;text.textSize=62f;text.alpha=(255*min(1f,p*4)).toInt();c.drawText(s.title,80f,150f+(1-e)*28f,text)
  muted.typeface=Typeface.DEFAULT;muted.textSize=28f;muted.alpha=255;c.drawText(s.subtitle,80f,202f+(1-e)*28f,muted)
  when(s.kind){0->hook(c);1->line(c,p);2->basket(c,p);3->pressure(c);4->cpi(c)};c.restore() }
 private fun hook(c:Canvas){text.color=Color.WHITE;text.textSize=150f;c.drawText("₹100",120f,440f,text);accent.color=Color.rgb(124,92,255);c.drawRoundRect(500f,310f,1100f,450f,28f,28f,accent);text.color=Color.WHITE;text.textSize=52f;c.drawText("buys less",620f,395f,text);text.textSize=24f;c.drawText("over time",625f,432f,text)}
 private fun line(c:Canvas,p:Float){val path=Path();for(i in 0..80){val x=140+950*i/80f;val y=550-250*(i/80f)*(0.35f+0.65f*p);if(i==0)path.moveTo(x,y)else path.lineTo(x,y)};val q=Paint(1);q.color=Color.rgb(124,92,255);q.style=Paint.Style.STROKE;q.strokeWidth=10f;c.drawPath(path,q);text.color=Color.WHITE;text.textSize=34f;c.drawText("general price level",140f,620f,text)}
 private fun basket(c:Canvas,p:Float){val labs=arrayOf("Milk","Rice","Fuel");val vals=floatArrayOf(45f,35f,20f);for(i in 0..2){val x=120+i*350f;val q=Paint(1);q.color=Color.rgb(124,92,255);val h=vals[i]*(1+.8f*p);c.drawRoundRect(x,490-h,x+180,490f,18f,18f,q);text.color=Color.WHITE;text.textSize=26f;c.drawText(labs[i],x,535f,text)};text.textSize=34f;c.drawText("Same ₹100 → smaller basket",120f,615f,text)}
 private fun pressure(c:Canvas){pill(c,130f,"DEMAND","more buyers");pill(c,700f,"COST","inputs get dearer")}
 private fun pill(c:Canvas,x:Float,a:String,b:String){accent.color=Color.rgb(124,92,255);c.drawRoundRect(x,340f,x+450f,470f,28f,28f,accent);text.color=Color.WHITE;text.textSize=30f;c.drawText(a,x+32,395f,text);text.textSize=22f;c.drawText(b,x+32,435f,text)}
 private fun cpi(c:Canvas){val labs=arrayOf("Food","Housing","Transport","Other");for(i in labs.indices){val x=110+i*275f;val q=Paint(1);q.color=if(i%2==0)Color.rgb(124,92,255)else Color.rgb(55,60,70);c.drawRoundRect(x,350f,x+220f,475f,24f,24f,q);text.color=Color.WHITE;text.textSize=24f;c.drawText(labs[i],x+25,425f,text)};text.textSize=34f;c.drawText("CPI = cost of a representative basket",110f,575f,text)}
 override fun onDraw(c:Canvas){super.onDraw(c);val now=SystemClock.uptimeMillis();drawFrame(c,now-start);if(playing){if(now-start>=project.scenes[sceneIndex].durationMs){if(sceneIndex<sceneCount-1){sceneIndex++;start=now}else playing=false};postInvalidateDelayed(16)}}
}

class VideoExporter(private val ctx:android.content.Context){
 fun render(p:VideoProject):File{val dir=File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),"VideoForge");dir.mkdirs();val out=File(dir,"inflation_for_beginners.mp4")
  val fmt=MediaFormat.createVideoFormat("video/avc",p.width,p.height);fmt.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);fmt.setInteger(MediaFormat.KEY_BIT_RATE,6000000);fmt.setInteger(MediaFormat.KEY_FRAME_RATE,p.fps);fmt.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,1)
  val codec=MediaCodec.createEncoderByType("video/avc");codec.configure(fmt,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);val surface=codec.createInputSurface();codec.start();val mux=MediaMuxer(out.absolutePath,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);var track=-1;var started=false;val info=MediaCodec.BufferInfo();val view=VideoCanvasView(ctx);var frame=0;val total=p.scenes.size*p.fps*5
  fun drain(){while(true){val s=codec.dequeueOutputBuffer(info,1000);when{s==MediaCodec.INFO_TRY_AGAIN_LATER->return;s==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED->{if(!started){track=mux.addTrack(codec.outputFormat);mux.start();started=true}};s>=0->{val b=codec.getOutputBuffer(s);if(b!=null&&info.size>0&&started){b.position(info.offset);b.limit(info.offset+info.size);mux.writeSampleData(track,b,info)};val eos=info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM!=0;codec.releaseOutputBuffer(s,false);if(eos)return}}}}
  try{while(frame<total){view.sceneIndex=(frame/(p.fps*5)).coerceIn(0,p.scenes.lastIndex);val cv=surface.lockCanvas(null);try{view.drawFrame(cv,(frame%(p.fps*5))*1000L/p.fps)}finally{surface.unlockCanvasAndPost(cv)};drain();frame++};codec.signalEndOfInputStream();drain()}finally{if(started)mux.stop();mux.release();codec.stop();codec.release();surface.release()};return out}
}
