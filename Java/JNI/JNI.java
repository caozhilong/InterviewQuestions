
JNI ---------- JAVA Native Interface
JAVA
boolean function( int i , double d ,char c)
{
	...
}

javap -s -private cn.itcast.TestNative

env->CallBooleanMethod(obj , id_function , 100L , 3.44 , L'3');
jvalue * args = new jvalue[3];
args[0].i = 100L;
args[1].d = 3.44;
args[2].c = L'3';

env->CallBooleanMethod(obj , id_function , args);
delete [] args;

//CallNonVirtual<TYPE>Method
有如下java代码
public class Father
{
	public void function()
	{
		System.out.println("Father:function");
	}
}

public class Child extends Father
{
	public void function()
	{
		System.out.println("Child:function");
	}
}

Father p = new Child();
p.function();
//在Java中调用Child的方法
若想要调用 父类的function是不可能的在java中


有如下C++代码

public class Father
{
	public ：
	void function()
	{
		System.out.println("Father:function");
	}
};

public class Child extends Father
{
	public ：
	void function()
	{
		System.out.println("Child:function");
	}
};

在这段代码中调用的是哪个类的成员函数
Father * p = new Child();
p->function();
调用  父类的成员函数function


public class Father
{
	public ：
	virtual void function()
	{
		System.out.println("Father:function");
	}
};

public class Child extends Father
{
	public ：
	void function()
	{
		System.out.println("Child:function");
	}
};

Father * p = new Child();
p->function();

调用子类成员函数function

在JNI 中定义的CallNonVirtual<TYPE>Method就能实现子类调用父类方法的功能。如果想要调用一个对象的父类的方法，而不是子类的这个方法的话，就可以使用CallNonVirtual<TYPE>Method

要使用它，首先要取得父类及要调用的父类方法的jmethodID。然后传入到这个函数就能通过子类对象呼叫被覆盖(override)的父类的方法



	jfieldID id_p = env->GetFieldID(clazz_TestNative , "p" , "Lcn/itcast/Father;");
	jobject p = env->GetObjectField( obj , id_p );

	jclass clazz_Father = env->FindClass("cn/itcast/Father");
	jmethodID id_Father_function = env->GetMethodID(clazz_Father , "function" , "()V");


	env->CallVoidMethod( p ,id_Father_function);
//Child:function
相当于
Java:
	Father p = tst.p;
	p.function();
	
env->CallNonvirtualVoidMethod( p , clazz_Father , id_Father_function);
//Father:function


0x004 JNI 本地代码中创建JAVA的对象--NewObject
	1.使用函数NewObject可以用来创建Java对象
	2.GetMethodID 能够取得构造方法的jmethodID,如果传入的要取得的方法的名称设定为"<init》"就能取得苟傲方法
	构造方法的方法返回值的类型始终为void
example：
		jclass clazz_date = env->FindClass("java/util/Date");	
		jmethodID mid_date = env->GetMethodID(clazz_date,"<init>","()V");
		jobject now = env->NewObject(clazz_date,mid_date);
		

必须将生成dll加入系统变量的path中

0x004_2 JAVA对象的创建 --- AllocObject
使用函数AllocObject可以根据传入的jclass创建一个JAVA对象，但是他的状态是非初始化的，在使用这个对象之前要用CallNonvirtualVoidMethod来调用该jclass的构建函数。这样可以延迟构造函数的调用

	jclass clazz_str = env -> FindClass("java/lang/String");
	jmethodID methodID_str = env ->GetMethodID(clazz_str , "<init>" , "([C)V");
	
	//预先创建一个没有初始化的字符串
	jobject string = env ->AllocObject(clazz_str);
	
	//创建一个4个元素的祖父数组，然后复制
	jcharArray arg = env ->NewCharArray(4);
	env->SetCharArrayRegion(arg , 0, 4,L"清原卓也");
	
	//呼叫构建ID
	env->CallNonvirtualVoidMethod(string , clazz_str , methodID_str ,arg);
	
	jclass clazz_this = env ->GetObjectClass(obj);
	
	//这里假设这个对象在类中有定义
	static String STATIC_STR;
	jfieldID fieldID_str = env->GetStaticFieldID(clazz_this ,"STATIC_STR" , "Ljava/lang/String;");
	env->SetStaticObjectField(clazz_str , fieldID_str , string);

Java字串<-->C/C++的字串
java：Unicode （UTF-16）	一个字符占两个字节，不论中文英文
C/C++: 英文占一个中文占两个
详情见桌面


0x005 其他的字符串函数

jstring NewString ( const jchar* str , jsize len);
jstring NewStringUTF(const char * str );
jsize GetStringLength(jstring str);
jsize GetStringUTFLength(jstring str);

//手动设置path
set path=%path%;C:\Program Files (x86)\Java\jdk1.7.0_01\bin;
1
	//把java的字符串转换成本地的字符串
    const jchar* jstr =	env->GetStringChars( j_msg ,NULL);
2 
	//把java的字符串转换成本地的字符串，增加指向回本地字符串的可能性
	//const jchar* jstr =	env->GetStringCritical( j_msg ,NULL);
3
	//使用windows的字符串到java,不需要java虚拟机分配内存
	jsize len = env->GetStringLength(j_msg);
	jchar * jstr = new jchar[len+1];
	//如不设置会出现乱码，分配内存没有结尾符,java中未提及---len+1
	jstr[len] = L'\0';
	env->GetStringRegion(j_msg , 0 ,len ,jstr);
	
java中string是常量不允许被修改,c++可以修改
	
JNIEXPORT void JNICALL Java_com_kiyo_MainTest_callCppFunction  (JNIEnv *env , jobject obj)
{
	//object is a LclassName; 
	jfieldID fid_msg =  env->GetFieldID(env->GetObjectClass(obj) , "message" , "Ljava/lang/String;");
	//
	jstring j_msg =(jstring) env->GetObjectField( obj , fid_msg);

	//把java的字符串转换成本地的字符串
	//const jchar* jstr =	env->GetStringChars( j_msg ,NULL);

	//把java的字符串转换成本地的字符串，增加指向回本地字符串的可能性
	//const jchar* jstr =	env->GetStringCritical( j_msg ,NULL);


	//使用windows的字符串到java
	jsize len = env->GetStringLength(j_msg);
	jchar * jstr = new jchar[len+1];
	//如不设置会出现乱码，分配内存没有结尾符,java中未提及---len+1
	jstr[len] = L'\0';
	env->GetStringRegion(j_msg , 0 ,len ,jstr);

	//MessageBox((HWND)"这是一个有标题的消息框！",(LPCWSTR)"标题"); 
	//MessageBoxW(NULL ,(const wchar_t*)jstr ,L"Title" ,MB_OK);
	
	//读取字符串进行修改，重新设定到JAVA的字符串,会拷贝到wstr
	wstring wstr((const wchar_t*)jstr);
	
	delete [] jstr;

	//使用标准库里面的reverse倒着输出
	std::reverse(wstr.begin(),wstr.end());

	//生成一个新的字串，然后把他的值指向他
	jstring j_new_str = env->NewString((const jchar*)wstr.c_str(),(jint)wstr.size());
	//释放字符串
	//env->ReleaseStringChars( j_msg ,jstr);

	//2释放字符串
	//2 
	//env->ReleaseStringCritical( j_msg ,jstr);

	//设定某一个字串
	env->SetObjectField(obj ,fid_msg ,j_new_str);

	//const jchar* == const wchar_t*;

}

0x006 在java中处理数组
		数组分为两种
			1.基本类型的数组
			3.对象类型(Object[])的数组

		在JNI中		一个能通用于两种不同类型数组的函数:
			GetArrayLength( jarray array)
		
	1.处理基本类型的数组
		（1.处理基本类型的数组的时候也是跟处理字符串类似，有很多相似额函数
		（2.Get<TYPE>ArrayElements(<TYPE>Array arr , jboolean* isCopied);这类函数可以吧JAVA基本类型的数组转换到C/C++中的数组，有两种处理方式
			一,拷贝一份传回本地代码
			二,把指向JAVA数组的指针直接传回到本地代码，处理完本地化的数组后，通过Release<TYPE>ArrayElements来释放数组
		（3.Release<TYPE>ArrayElements(<TYPE> Array arr,<TYPE>* array ,jint mode)用这个函数可以选择将如何处理JAVA跟C++的数组。是提交，韩式撤销,内存释放函数不释放等。
	mode可以取下面的值:
		0  			-----> 对JAVA的数组进行更新并释放C/C++的数组
		JNI_COMMIT	-----> 对JAVA的数组进行更新但不释放C/C++的数组
		JNI_ABORT   -----> 对JAVA的数组进行不更新但释放C/C++的数组
		
		(4. GetPrimitiveArrayCritical(jarray arr, jboolean* isCopied);
			ReleasePrimitiveArrayCritical(jarray arr, void* array ,jint mode);
			也是JDK1.2出来的，为了增加直接传回指向JAVA数组的指针而加入额函数。同样的,也会有同GetStringCritical的死锁的问题
		(5. Get<TYPE>ArrayRegion(<TYPE>Array arr, jsize start ,jsize len , <TYPE>* buffer);在C/C++预先开辟一段内存，然后把JAVA基本类型的数组拷贝到这段内存中跟GetStringRegion的原理类似
	   (6.操作数组
			Set<TYPE>ArrayRegion(<TYPE>Array arr, jsize start ,jsize len , <TYPE>* buffer);把Java基本类型的数组中的指定范围的元素用C/C++的数组中的元素来赋值
		(7.<TYPE>Array New<TYPE>Array(jsize sz)指定一个长度然后返回相应的Java基本类型的数组
	
	2.处理对象类型的数组
		(Object[])JNI没有提供直接把JAVA	的对象类型数组(Object[])直接转到C++中的jobject[]数组的函数。而是直接通过Get/SetObjectArrayElement这样的函数对Java的object[]数组进行操作
		jobject GetObjectArrayElement(jobjectArray array, jsize index) { return functions->GetObjectArrayElement(this,array,index); }
		
/*
 * Class:     com_kiyo_MainTest
 * Method:    callCppFuncyion
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_kiyo_MainTest_callCppFuncyion  (JNIEnv * env, jobject obj)
{
	jfieldID fid_arrays = env->GetFieldID( env->GetObjectClass(obj),"arrays","[I");
	jintArray jint_arr =(jintArray) env->GetObjectField(obj,fid_arrays);

	jint* int_arr = env->GetIntArrayElements(jint_arr ,NULL);
	jsize len = env->GetArrayLength(jint_arr);

	std::sort(int_arr , int_arr+len);
/*	for(jsize i=0; i<len; ++i)
	{
		cout<<int_arr[i] << endl;
	}
*/
	env->ReleaseIntArrayElements(jint_arr , int_arr , 0);

}		
	

0x007 JNI全局引用/局部引用/弱全局引用
	1.从Java虚拟机创建的对象传回到本地的C/C++代码时，会产生引用，更具JAVA的垃圾回收机制，只要有引用存在就不会出发该引用指向的JAVA对象的垃圾回收。
	2.这些引用在JNI中分三种
		全局引用(Global Reference)
		局部引用(Local Reference)
		弱全局引用(Weak Global Reference) since JDK1.2
		2.1 局部引用 
			（1.最常见的引用累心。基本上通过JNI返回的引用都是局部引用。
				例如使用NewObject就会返回创建出来的局部引用，局部引用只在native函数中有效，所有在该函数中产生的局部引用，都会在函数返回的时候自动释放(freed)。也可以使用DeleteLocalRef函数手动释放该引用
			（2.既然局部引用能够在函数返回的时候自动释放，为什么还需要DeleteLocalRef函数
				答：实际上局部引用存在，就会方式其指向的对象被垃圾回收，油气是当一个局部引用在指向很庞大的对象，或是在一个循环中生成可局部引用，最好的做法就是使用完该对象后，或者在循环尾部把这个引用释放掉，以确保在垃圾回收器被触发的时候回收、
			（3.在局部引用的有效期中，可以传递到别的本地函数中，要强调的是他的有效期仍然只是一次的JAVA本地函数调用中，所以千万不能用C++全局变量保存他或是把他定义为C++静态局部变量
		2.2 全局引用
			（1.全局引用可以宽约当前线程，在多个native函数中有效，不过需要编程人员手动释放该引用。全局引用存在期间会防止在java的垃圾回收器的回收
			（2.全局引用与局部引用不同，全局引用的创建不是由JNI自动创建的，全局引用是需要调用NewGlobalRef函数，而释放它需要使用ReleaseGlobalRef函数
		2.3	 弱全局引用
			（1.java 1.2 新出来的功能，与全局引用相似，创建跟删除都需要编程人员来进行。这种引用与全局引用一样可以在本地代码有效，也跨越多线程有效，不一样的是，这种引用将不会阻止垃圾回收器的回收这个引用所指的对象
			
			tips:使用该方式必须特别小心，小心该回收器的已被回收，或者不存在
			（2.使用NewWeakGlobalRef跟ReleaseWeakGlobalRef来产生和解除引用
			
			关于引用的函数:
				jobject NewGlobalRef( jobject obj); //创建全局
				jobject NewLocalRef( jobject obj);  //创建局部
				jobject NewWeakGlobalRef(jobject obj); //创建弱全局
				void DeleteGlobalRef(jobject obj);
				void DeleteLocalRef( jobject obj);
				void DeleteWeakGlobalRef(jobject obj);
				jboolean IsSameObject(jobject obj1 , object obj2);
				
				//这个函数对于弱全局引用还有一个特别的功能
				//把NULL传入要比较的对象中，就能判断弱全局引用所指向的JAVA对象是否被回收
				
	3.缓存jfieldID、jmethodID
		(1. 取得jfieldID跟jmethodID的时候回通过属性/方法名称加上签名来查询相应的jfieldID/jmethodID.这种查询相对来说开销较大。我们可以将这些FieldID/MethodID缓存起来，这样我们只需要查询一次,以后就使用缓存起来的FieldID/MethodID了
	
		（2.下面两种缓存方式
			1.在用的时候缓存
				(Caching at the Point of Use)
			2.在Java类初始化时缓存
				(Caching at the Defining Class's Initializer)
	
		 (3.缓存jfieldID/jmethodID在第一次使用的时候缓存
			1.在native code 使用 static 局部变量来保存已经查询过的id。这样就不会再每次的函数调用时查询，而只要第一次查询成功后就保存起来了。
			2.不过早这种情况下就不得不考虑多线程同时呼叫此函数时可能会招致同时查询的危机，不过这种情况是无害的，因为查询同一个属性/方法的ID通常返回的是一样的值.
			//若查询多次，会查询一个jfield其结果不会发生变换只会等待
			JNIEXPORT void JNICALL Java_Test_native  (JNIEnv * env, jobject obj)
				{
					static jfieldID jfieldID_string = NULL;
					jclass clazz =env->GetObjectClass(obj);
					
					if(jfieldID_string == null)
					{
						jfieldID_string = env->GetFieldID(clazz , "string" ,"Ljava/lang/String;");
					}

				}
			（4. 缓存jfieldID/jmethodID在JAVA类初始化的时候缓存
				1.更好的一个方式就是在任何native函数调用前把id全部存起来
				2.我们可以让Java在第一次加载这个类的时候首先调用本地代码出事所有的jfieldID/jmethodID,这样的话就可以省去多次确定id是否存在的语句，当然这些jfieldID/jmethodID是定义在C/C++的全局。
				
				3.使用这种方式还有好处，当JAva类卸载或是重新加载的时候也会重新呼叫该本地代码来重新计算IDs
					demo：
					在java类中
					public class TestNative
					{
						static 
						{
							initNativeIDs();
						}
						static native void initNativeIDs()；
						
						int propInt = 0;
						String propStr = "";
						public native void otherNative();
						//... other code
					}
				C/C++代码中:
				//global variables
				jfieldID g_propInt_id = 0;
				jfieldID g_propStr_id = 0;
				JNIEXPORT void JNICALL Java_TestNative_initNatibeIDs(JNIEnv* env , jobject clazz)
				{
					g_propInt_id = GetFieldID(clazz , "propInt" ,"I");
					g_propStr_id = GetFieldID(clazz , "propStr" , "Ljava/lang/String;");
					
				}
				JNIEXPORT void JNICALL Java_TestNative_otherNative(JNIEnv * env , jobject obj)
				{
					//get field with g_propInt_id/g_propStr_id ...
				}


总结
	1.最简单的Java调用C/C++	函数的方法
	2.取得方法/属性的Id，学会了取得/设置属性，还有Java函数的调用
	3.Java/C++之间的字符串的转换问题
	4.在C/C++下如何操作Java的数组
	5.三种引用方式
	6.如何缓存属性/方法的ID
	

	使用JNI的两个弊端
		1. 使用了JNI，那么这个Java Application 将不能跨平台了。如果要移植到别的平台上，那么native代码就需要重新编写。
		2. Java是强类型的语言，而C/C++不是。因此，必须在写JNI更加小心
	总之,必须在构建Java程序的时候，尽量少用本地代码。
	
	其他
		1.异常处理
		2.C/C++如何启动JVM
		3.JNI跟多线程
		
		4.The Java Native Interface Programmer's Guide and Specification
		5.JNI++ User Guide













	