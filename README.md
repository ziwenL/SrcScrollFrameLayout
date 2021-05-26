# SrcScrollFrameLayout <a href="https://blog.csdn.net/lzw398756924/article/details/106101545" rel="nofollow">博客地址</a>
<h3 >基本介绍</h3>
<p>　　仿小红书登陆页面背景图无限滚动 FrameLayout <p>

<h3>显示效果</h3>
<img  src="https://img-blog.csdnimg.cn/20200514114713870.gif?raw=true" alt="实际效果" />

<h3>属性说明</h3>
<h5>自定义属性</h5>

|name|format|default|required|description|
|:---:|:---:|:---:|:---:|:---:|
| src | reference | | false |设置图片背景
| maskLayerColor | color  | | false | 设置遮罩层颜色，建议带透明度
| isScroll | boolean | true | false | 设置是否滚动
| speed | integer | ordinary |false| 滚动速度 slow、ordinary、fast ，也可手动赋值 ( 建议取值区间 [1,50] ）
| scrollOrientation| integer | toTop |false| 滚动方向 toTop、toBottom、toLeft、toRight ，默认是上移

<h5>自定义方法</h5>

|name|description|
|:---:|:---:|
| setSrcBitmap (Bitmap srcBitmap) | 设置背景图 bitmap |
| startScroll ( ) | 开始滚动 |
| stopScroll ( ) | 停止滚动 |
| changeScrollOrientation ( ) | 切换滚动方向 , 从上移、下移、左移和右移按顺序切换 |
| setScrollOrientation (@ScrollOrientation int scrollOrientation) | 设置滚动方向 |

<h3>使用方法</h3>
<ul>
<li>
<p>1.复制类 <a href="https://github.com/ziwenL/SrcScrollFrameLayout/blob/master/library/src/main/java/com/ziwenl/library/widgets/SrcLoopScrollFrameLayout.kt" rel="nofollow">SrcLoopScrollFrameLayout</a> 到项目中，并将其<a href="https://github.com/ziwenL/SrcScrollFrameLayout/blob/master/library/src/main/res/values/attrs.xml" rel="nofollow"> 自定义属性 </a>复制至 attrs 中</p>
</li>
<li>
<p>2.布局中引用 SrcLoopScrollFrameLayout ，通过 src、maskLayerColor 属性设置需要滚动的背景图和遮罩层颜色，然后当成普通 FrameLayout 来用即可</p>
</li>
</ul>

<h3>About Me<h3>
<ul>
<li>
<p>Email: ziwen.lan@foxmail.com</p>
</li>
</ul>
