## SSM注解开发的高级知识讲解

**写在前边的话:**经过前面两篇文章[SSM的整合和一个案例带你快速入门SSM开发](http://codingxiaxw.cn/2016/11/15/45-smm%E5%BF%AB%E9%80%9F%E5%85%A5%E9%97%A8/)的讲解后，我们已经顺利的了解了利用SSM进行开发的基础知识，本篇文章将为你介绍更复杂的高级知识，例如数据回显、参数绑定list数据、图片的上传、json数据的请求和响应、validation分组校验、统一异常处理、RESTful支持以及拦截器的应用。


当然此篇文章也是在前篇文章开发的工程基础上进行讲解，详细讲解请点击这里前往我的博客[SSM注解开发的高级知识讲解](http://codingxiaxw.cn/2016/11/19/46-ssm%E9%AB%98%E7%BA%A7%E5%BC%80%E5%8F%91/)

## 开发环境
IDEA Spring3.x+SpringMVC+Mybatis  
没有用到maven管理工具。

## 1.数据回显
需求:当提交表单时出现错误，回到表单重新填写数据时需要将刚才提交的参数在页面回显。以往我们采用将数据放在request域中然后请求转发到表单页面，运用el表达式将数据回显。在springmvc中我们通过Model对象的addAttribute("id",id),将简单类型的数据传入request域(注意在进入修改页面的controller方法中和提交修改商品信息方法model.addAttribute方法设置的key一致)。通过Model的addAttribute("pojo",pojo)方法将pojo类型的数据也是传入到request域中，然后通过el表达式完成取出request域中的数据，这是pojo类型数据回显的第一种方法，如下:
```java
@RequestMapping("/editItemSubmit")
public String editItemSubmit(Model model,Integer id,ItemsCustom itemsCustom) throws Exception
{
   //进行数据回显
   model.addAttribute("id",id);
   model.addAttribute("item",itemsCustom);
   
   itemsService.updateItems(id,itemsCustom);
   return "editItem";
}
```

第二种方法是通过@ModelAttribute()注解的方式，括号内传入要回显的pojo对象，如我们这里要将itemsCustom传到request域所以我们这样写`@ModelAttribute(value="itemsCustom")`，然后便可以将代码中的将model添加到request域的代码`model.addAttribute("item",itemsCustom);`注释掉了。  

**@ModelAttribute()注解的另外一种用法:**将方法的返回值填入到request域然后显示在页面上。在ItemsController.java中加入方法:
```java
    //单独将商品类型的方法提取出来，将方法返回值填充到request域，在页面提示
    @ModelAttribute("itemsType")
    public Map<String,String> getItemsType() throws Exception{

        HashMap<String,String> itemsType=new HashMap<>();
        itemsType.put("001","data type");
        itemsType.put("002","clothes");
        return itemsType;
    }
```

然后我们在itemsList.jsp页面中加入如下标签进行页面的展示:
```xml
<td>
		<select>
			<c:forEach items="${itemsType}" var="item">

				<option value="${item.key}">${item.value}</option>

			</c:forEach>

		</select>
	</td>
```

成功使用@ModleAttribute注解将方法返回值返回到页面。其实这种注解的方式也是将数据填入到request域中，然后在页面中通过el表达式取出，同model.addAttribute()方法和modelAndView.addObject()方法。  

使用@ModelAttribute将公用的取数据的方法返回值传到页面，不用在每一个controller方法通过Model将数据传到页面。

## 2.将页面信息绑定到参数集合类型中

前面的基础知识讲解中我们都只是将简单类型和pojo类型的数据绑定到了参数中。接下来讲解高级知识的绑定。  

### 2.1将页面信息绑定到数组参数中
需求:在商品查询列表页面，用户选择要删除的商品，批量删除商品。  

在controller方法中如何将批量提交的数据绑定成数组类型。  

页面的定义,在itemsList.jsp中每件商品前面添加一个checkbox:
```xml
<td><input type="checkbox" name="delete_id" value="${item.id}"> </td>
```

在上方添加一个批量删除的按钮:
```xml
<input type="button" value="批量删除" onclick="deleteItems()">
```
因为我们form的action是点击查询后返回到itemsList.action，所以这里我们点击delete_id按钮应该返回到另一个action所以需要编写一个js脚本对该按钮进行事件监听。


以及点击这个按钮响应的事件:
```xml
	<script type="text/javascript">
		function deleteItems() {
			//将form的action指向删除商品的地址
			document.itemsForm.action="${pageContext.request.contextPath}/items/deleteItems.action";

			//进行form提交
			document.itemsForm.submit();

		}
	</script>
```

然后需要在contronller中添加deleteItems()方法:
```java
    //删除商品
    @RequestMapping("/deleteItems")
    public String deleteItems(Integer[] delete_id) throws Exception
    {
        //调用serive方法删除商品
        //这里我们就是简单的介绍完成将页面的信息绑定到数组中，所以service的方法你可以自己去完成

        return "success";
    }
```

然后定义一个success.jsp页面输出成功删除的信息即可。

### 2.2绑定页面信息到List<Object>中
需求:批量修改商品信息提交。先进入批量修改商品页面，填写信息，点击提交。  

页面的定义,新建一个editItemsList.jsp页面，内容如下:
```xml
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"  prefix="fmt"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>批量修改商品查询</title>
	<script type="text/javascript">
		function updateItems() {
			//将form的action指向删除商品的地址
			document.itemsForm.action="${pageContext.request.contextPath}/items/deleteItems.action";

			//进行form提交
			document.itemsForm.submit();

		}
	</script>
</head>
<body> 
<form name="itemsForm" action="${pageContext.request.contextPath }/items/queryItem.action" method="post">
查询条件：
<table width="100%" border=1>
<tr>
	<td>
		<select>
			<c:forEach items="${itemsType}" var="item">

				<option value="${item.key}">${item.value}</option>

			</c:forEach>

		</select>
	</td>
<td><input type="submit" value="查询"/>
<input type="button" value="批量修改提交" onclick="updateItems()">
</td>
</tr>
</table>
商品列表：
<table width="100%" border=1>
<tr>
	<td>商品名称</td>
	<td>商品价格</td>
	<td>生产日期</td>
	<td>商品描述</td>
	<td>操作</td>
</tr>
<c:forEach items="${itemsList }" var="item" varStatus="s">
<tr>
	<td><input type="text" name="itemsList[${s.index}].name" value="${item.name}"> </td>
	<td><input type="text" name="itemsList[${s.index}].price" value="${item.price}"> </td>
	<td><fmt:formatDate value="${item.createtime}" pattern="yyyy-MM-dd HH:mm:ss"/></td>
	<td>${item.detail }</td>
	
	<td><a href="${pageContext.request.contextPath }/items/editItems.action?id=${item.id}">修改</a></td>

</tr>
</c:forEach>

</table>
</form>
</body>

</html>
```

在controller中加入如下两个方法:
```java
 //批量修改商品查询
    @RequestMapping("/editItemsList")
    public ModelAndView editItemsList() throws Exception {
        //调用servie来查询商品列表
        List<ItemsCustom> itemsList=itemsService.findItemsList(null);

        ModelAndView modelAndView=new ModelAndView();
        modelAndView.addObject("itemsList",itemsList);
        //指定逻辑视图名itemsList.jsp
        modelAndView.setViewName("editItemsList");

        return modelAndView;
    }

    //批量修改商品的提交

    @RequestMapping("/editItemsListSubmit")
    public String editItemsListSubmit(ItemsQueryVo itemsQueryVo) throws Exception{
        return "success";
    }
```

在xml页面中的name属性值的定义是很重要的，因为在我们提交表单后该数据是会绑定到后台中的方法参数中的，这里我们使用`itemsList[${s.index}].name`作为name的属性值，所以我们在controller的submit方法中添加了一个ItemsQueryVo的参数，若想完成绑定则需要在ItemsQueryVo中定义一个itemsList的属性，如下:
```java
public class ItemsQueryVo {
	
	//商品信息
	private ItemsCustom itemsCustom;

	//定义一个list
	private List<ItemsCustom> itemsList;

	public List<ItemsCustom> getItemsList() {
		return itemsList;
	}

	public void setItemsList(List<ItemsCustom> itemsList) {
		this.itemsList = itemsList;
	}

	public ItemsCustom getItemsCustom() {
		return itemsCustom;
	}

	public void setItemsCustom(ItemsCustom itemsCustom) {
		this.itemsCustom = itemsCustom;
	}
}
```
注释:  
`itemsList:`controller方法形参包装类型中list的属性名。  
`itemsList[0]或itemsList[1]...`的[]中是序号，从0开始。  
`itemsList[].name:`中name就是controller方法形参包装类型中list中pojo的属性名。(有点绕口)  

## 3.商品图片的上传
需求:在商品修改页面，增加图片上传的功能。  

操作流程:用户进入商品修改页面,上传图片,点击提交（提交的是图片和商品信息,再次进入修改页面，图片在商品修改页面展示。  

### 3.1图片存储问题
切记：不要把图片上传到工程目录 ，不方便进行工程维护。实际电商项目中使用专门图片服务器(http，比如apache、tomcat)。本教程使用图片虚拟目录，通过虚拟目录访问硬盘上存储的图片目录。  

我用的是开发工具为IDEA，虚拟目录的设置为,1.打开tomcat的设置:  
![](http://od2xrf8gr.bkt.clouddn.com/%E5%B1%8F%E5%B9%95%E5%BF%AB%E7%85%A7%202016-11-19%20%E4%B8%8B%E5%8D%883.38.02.png)  

2.点击External Source:  
![](http://od2xrf8gr.bkt.clouddn.com/%E5%B1%8F%E5%B9%95%E5%BF%AB%E7%85%A7%202016-11-19%20%E4%B8%8B%E5%8D%883.39.28.png)  

3.进入到如下页面:  
![](http://od2xrf8gr.bkt.clouddn.com/%E5%B1%8F%E5%B9%95%E5%BF%AB%E7%85%A7%202016-11-19%20%E4%B8%8B%E5%8D%883.39.43.png)  

选择硬盘上的一个路径作为虚拟目录的位置，以后上传的图片都会放在此路径上。并在Application context中添加一个在服务器上访问的路径别名，我这里设置的为`/pic`。若想访问该目录下的图片，则在浏览器网站上输入:`http://localhost:8080/pic/图片名称`，(不需要写项目名称)，即可在浏览器中成功访问到该图片。  

**注意:**图片目录中尽量进行目录分级存储，提高访问速度（提交i/o）

### 3.2配置图片上传解析器
springmvc使用commons-fileupload进行图片上传。  

需要导入的jar包为:commons-fileupload-1.2.2.jar和依赖包commons-io-2.4.jar。  

然后在springmvc中配置图片上传解析器:
```xml
 <!-- 文件上传
    CommonsMultipartResolver依赖我们传入的fileupload jar包-->
    <bean id="multipartResolver"
          class="org.springframework.web.multipart.commons.CommonsMultipartResolver">
        <!-- 设置上传文件的最大尺寸为5MB -->
        <property name="maxUploadSize">
            <value>5242880</value>
        </property>
    </bean>
```

### 3.3编写上传图片的页面
在editItem.jsp中添加图片的文件表单项:
```xml
	<tr>
		<td>商品图片</td>
		<td>
			<c:if test="${itemsCustom.pic}!=null">
				<img src="/pic/${itemsCustom.pic}" width="100" height="100">
				<br/>
			</c:if>
			<input type="file" name="pictureFile">
		</td>
	</tr>
```

另外涉及到文件表单项的地方需要在form标签中添加`enctype="multipart/form-data"`的属性。  

### 3.4编写controller方法
修改edititemSubmit方法，添加参数图片进行页面的图片信息与参数的绑定:
```java
 @RequestMapping("/editItemSubmit")
    public String editItemSubmit(Model model,Integer id,
                                 @Validated(value = {ValidGroup1.class}) @ModelAttribute(value = "itemsCustom") ItemsCustom itemsCustom,
                                 BindingResult bindingResult,
                                 //上传图片
                                 MultipartFile pictureFile
                                 ) throws Exception
    {
     //进行数据回显
        model.addAttribute("id",id);
//        model.addAttribute("item",itemsCustom);

        //进行图片的上传
        if (pictureFile!=null&&pictureFile.getOriginalFilename()!=null&&pictureFile.getOriginalFilename().length()>0)
        {
            //图片上传成功后，将图片的地址写到数据库
            String filePath="/Users/codingBoy/Pictures/";//它的值要同你设置虚拟目录时涉及的目录路径一致，
            String originalFilename=pictureFile.getOriginalFilename();

            String newFileName= UUID.randomUUID()+originalFilename.substring(originalFilename.lastIndexOf("."));

            //新文件
            File file=new File(filePath+newFileName);

            //将内存中的文件写入磁盘
            pictureFile.transferTo(file);

            //图片上传成功
            itemsCustom.setPic(newFileName);
        }


        itemsService.updateItems(id,itemsCustom);
        //请求转发
//        return "forward:queryItems.action";


       return "editItem";
        //重定向
        //return "redirect:queryItems.action";
    }
```
然后运行程序，在修改商品信息的页面上传图片，成功上传后便可在你设置的虚拟目录中找到你上传的图片。  

## 4.json数据的交互
需求:json数据格式是比较简单容易理解，json数据格式常用于远程接口传输，http传输json数据，非常方便页面进行提交/请求结果解析，对json数据的解析。

### 4.1springmvc解析json需要加入的json解析包
Springmvc默认用MappingJacksonHttpMessageConverter对json数据进行转换，需要加入jackson的包:jackson-core-sal-1.9.11.jar和jackson-mapper-asl-1.9.11.jar。  

### 4.2在处理器适配器中加入MappingJacksonHttpMessageConverter配置
在springmvc的处理器适配器的配置中加入如下配置信息:
```xml
  <!--加入json数据的消息转换器
        MappingJacksonHttpMessageConverter依赖jackson的两个jar包-->
        <property name="messageConverters">
            <list>
                <bean class="org.springframework.http.converter.json.MappingJacksonHttpMessageConverter"></bean>
            </list>
        </property>
```

### 4.3@RequestBody和@ResponseBody注解  

@RequestBody:将请求的json数据转成java对象。  
@ResponseBody:将java对象转成json数据输出。  

图解:![](http://od2xrf8gr.bkt.clouddn.com/%E5%B1%8F%E5%B9%95%E5%BF%AB%E7%85%A7%202016-11-19%20%E4%B8%8B%E5%8D%884.02.01.png)  

### 4.4测试1:请求的是json数据，响应的也是json数据
在controller包下写一个json测试类的controller叫jsonTest.java，代码如下:
```java
@Controller
public class JsonTest
{

    //请求的json响应json，请求商品信息(该信息为json格式，在页面中通过ajax向写入用户请求的json信息，需要加入@RequestBody注解)，商品信息用json格式输出商品信息(请求的url实际返回的是itemsCustom对象，但由于使用了@ResponseBody就将返回的pojo对象转换成了json格式的数据)
    @RequestMapping("/requestJson")
    public @ResponseBody ItemsCustom requestJson(@RequestBody ItemsCustom itemsCustom) throws Exception{

        return itemsCustom;
    }

}
```

在web包下创建一个请求数据的页面jsonTest.jsp,代码如下:
```xml
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>json测试</title>
<script type="text/javascript" src="${pageContext.request.contextPath }/js/jquery-1.4.4.min.js"></script>
<script type="text/javascript">

//请求json响应json
function requestJson(){
	$.ajax({
		url:"${pageContext.request.contextPath }/requestJson.action",
		type:"post",
		contentType:"application/json;charset=utf-8",
		//请求json数据,使用json表示商品信息
		data:'{"name":"手机","price":1999}',
		success:function(data){
			
			alert(data.name);
		}
		
		
	});
	
	
}

</script>
</head>
<body>

<input type="button" value="请求json响应json" onclick="requestJson()"/>
<input type="button" value="请求key/value响应json" onclick="responseJson()"/>
</body>
</html>
```  

测试追踪，在浏览器中输入:`http://localhost:8080/SpringMvcMybatis/jsonTest.jsp`直接访问改页面，点击`请求json响应json`按钮，通过抓包的方式，我们看到服务器上放回的信息为:![](http://od2xrf8gr.bkt.clouddn.com/%E5%B1%8F%E5%B9%95%E5%BF%AB%E7%85%A7%202016-11-19%20%E4%B8%8B%E5%8D%884.16.17.png)


### 4.5测试2:请求key/value数据，响应的json数据
在jsonTest.java中加入方法:
```java
    //请求key/value(在页面中通过ajax写入用户想要请求的key/value信息，不需要加@RequestBody注解)，响应json(由于action返回的是itemsCustom对象，所以需要加入@ResponseBody注解将pojo对象转换为json格式响应给用户)
    @RequestMapping("/responseJson")
    public @ResponseBody ItemsCustom responseJson(ItemsCustom itemsCustom) throws Exception{

        return itemsCustom;
    }
```

在页面中加入如下代码:
```xml
//请求key/value响应json
function responseJson(){
	
	$.ajax({
		url:"${pageContext.request.contextPath }/responseJson.action",
		type:"post",
		//contentType:"application/json;charset=utf-8",
		//请求key/value数据
		data:"name=手机&price=1999",
		success:function(data){
			
			alert(data.name);
		}
		
		
	});
}
```

测试追踪，在浏览器中输入网址，点击`请求key/value响应json`按钮，通过抓包的方式我们发现丛服务器上返回过来的信息为:![](http://od2xrf8gr.bkt.clouddn.com/%E5%B1%8F%E5%B9%95%E5%BF%AB%E7%85%A7%202016-11-19%20%E4%B8%8B%E5%8D%884.20.04.png)  

可以看到，我们请求的是key/value数据，而服务器返回的信息是json格式的数据。  

### 4.6小结
如果前端处理没有特殊要求建议使用第二种，请求key/value，响应json，方便客户端解析请求结果。  

## 5validation校验(了解)
对前端的校验大多数通过js在页面校验，这种方法比较简单，如果对安全性考虑，还要在后台校验。  

springmvc使用JSR-303（javaEE6规范的一部分）校验规范，springmvc使用的是Hibernate Validator（和Hibernate的ORM无关）。  

### 5.1加入Hibernate Validator的jar
![](http://od2xrf8gr.bkt.clouddn.com/%E5%B1%8F%E5%B9%95%E5%BF%AB%E7%85%A7%202016-11-19%20%E4%B8%8B%E5%8D%884.23.53.png)  

### 5.2在处理器适配器中配置校验器  
在springmvc.xml中的自定义webBinder标签中加入如下标签:
```xml
  <property name="validator" ref="validator"/>
```

这里引入了一个validator，所以我们需要在springmvc.xml中还需要加入validator的配置:
```xml
<bean id="validator"
          class="org.springframework.validation.beanvalidation.LocalValidatorFactoryBean">
        <property name="providerClass" value="org.hibernate.validator.HibernateValidator" />
        <!-- 如果不指定则默认使用classpath下的ValidationMessages.properties -->
        <property name="validationMessageSource" ref="messageSource" />
    </bean>
    <!-- 校验错误信息配置文件 -->
    <bean id="messageSource"
          class="org.springframework.context.support.ReloadableResourceBundleMessageSource">
        <property name="basenames">
            <list>
                <value>CustomValidationMessages</value>
            </list>
        </property>
        <property name="fileEncodings" value="utf-8" />
        <property name="cacheSeconds" value="120" />
    </bean>
```

然后在src包下创建一个CustomValidationMessages.properties文件，内容为:
```xml
#校验提示信息:items.name.length.error要写在java代码中
items.name.length.error=商品名称的长度请限制在1到30个字符
items.createtime.is.notnull=请输入商品的生产日期
```

### 5.3校验规则
需求:商品信息提交时校验 ，商品生产日期不能为空，商品名称长度在1到30字符之间。  

另外首先需要知道的是规则应该在pojo对象的相应属性上规定。

修改pojo的Items.java内容如下:
```java
public class Items {
    private Integer id;

    @Size(min = 1,max = 30,message = "{items.name.length.error}")
    private String name;

    private Float price;

    private String pic;
    @NotNull(message="{items.createtime.is.notnull}")
    private Date createtime;

    private String detail;
}
省略了相应的setter和getter方法。
```

通过上面的操作我们定义了校验的规则，那么当违反了上述规则后我们该如何捕获错误呢?需要修改controller方法，在要校验的pojo前边加上@Validated注解，在editItemSubmit()方法中添加如下内容:
```java
  //商品提交页面
    //itemsQueryVo是包装类型的pojo
    //在@Validated中定义使用ValidGroup1组下边的校验
    @RequestMapping("/editItemSubmit")
    public String editItemSubmit(Model model,Integer id,
                                 @Validated @ModelAttribute(value = "itemsCustom") ItemsCustom itemsCustom,
                                 BindingResult bindingResult,
                                 //上传图片
                                 MultipartFile pictureFile
                                 ) throws Exception
    {
        //输出校验错误信息
        //如果参数绑定时出错
        if (bindingResult.hasErrors())
        {
            //获取错误
            List<ObjectError> errors=bindingResult.getAllErrors();

            model.addAttribute("errors",errors);

            for (ObjectError error:errors)
            {
                //输出错误信息
                System.out.println(error.getDefaultMessage());
            }

            //如果校验错误，仍然回到商品修改页面
            return "editItem";

        }
        
      ....
}
```

然后是在页面editItem.jsp中展示错误:
```xml
<!--错误信息-->
<c:forEach items="${errors}" var="error">
<div color="red">${error.defaultMessage}<br/></div>
</c:forEach>
```
运行程序，在修改商品信息的页面当我们没有输入日起或是将商品名称超过了20个字符时便不能完成页面的跳转而是在该修改页面显示错误信息。  

以上我们便通过Validation完成了校验。下面我们来谈谈分组校验:针对不同的controller方法通过分组校验达到个性化校验的目的。

### 5.4分组校验
需求:修改商品修改功能，只校验生产日期不能为空。  

第一步:创建分组接口,创建一个ValidGroup1.java接口:
```java
public interface ValidGroup1
{
    //接口不定义方法，相当于接口就是个类型，只标识哪些校验规则属于该ValidGroup1分组
}

```

第二步:定义校验规则属于哪个校验分组:  
```java
 //通过groups指定此校验属于哪个分组，当然可以指定多个分组
    @NotNull(message="{items.createtime.is.notnull}",groups = {ValidGroup1.class})
    private Date createtime;
```

而属性name的校验规则没有添加groups属性。  

第三步:在controller方法定义使用校验的分组。
```java
   public String editItemSubmit(Model model,Integer id,
                                 @Validated(value = {ValidGroup1.class}) @ModelAttribute(value = "itemsCustom") ItemsCustom itemsCustom,
                                 BindingResult bindingResult,
                                 //上传图片
                                 MultipartFile pictureFile
                                 ) throws Exception
    {}
```
此时运行程序，在编辑商品信息页面中只会进行日期是否为空的校验，而不能进行商品名称信息的校验。因为在pojo的name属性中我们没有指定该校验规则属于哪个分组，所以在controller的方法中就无法定义使用该校验规则的分组，也就无法进行商品名称的校验。  

## 6.统一异常处理
需求:一般项目中都需要作异常处理，基于系统架构的设计考虑，使用统一的异常处理方法。  

系统中异常类型有哪些？  

包括预期可能发生的异常、运行时异常（RuntimeException），运行时异常不是预期会发生的。  

针对预期可能发生的异常，在代码手动处理异常可以try/catch捕获，可以向上抛出。  

针对运行时异常，只能通过规范代码质量、在系统测试时详细测试等排除运行时异常。

### 6.1统一异常处理解决方案
#### 6.1.2自定义系统异常类
针对预期可能发生的异常，定义很多异常类型，这些异常类型通常继承于Exception。这里定义一个系统自定义异常类CustomException.java，用于测试:
```java
public class CustomException extends Exception
{

    //异常信息
    private String message;

    public CustomException(String message)
    {
        super(message);
        this.message=message;

    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
```

#### 6.1.2异常处理
要在一个统一异常处理的类中要处理系统抛出的所有异常，根据异常类型来处理。

统一异常处理的类是什么？

前端控制器DispatcherServlet在进行HandlerMapping、调用HandlerAdapter执行Handler过程中，如果遇到异常，都会调用这个统一异常处理类。当然该类只有实现HandlerExceptionResolver接口才能叫统一异常处理类。  

##### 6.1.2.1定义统一异常处理器类
创建一个CustomExceptionResolver.java文件并实现HandlerExceptionResolver接口，代码如下:
```java
public class CustomExceptionResolver implements HandlerExceptionResolver
{
    //前端控制器DispatcherServlet在进行HandlerMapping、
    // 调用HandlerAdapter执行Handler过程中，如果遇到异常就会执行此方法
    //参数中的handler是最终要执行的Handler，它的真实身份是HandlerMethod
    //ex就是接受到的异常信息
    @Override
    public ModelAndView resolveException(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Object handler, Exception ex) {
     return null;
}
```

##### 6.1.2.2配置统一异常处理器
在springmvc.xml文件中加入统一异常处理器的配置:
```xml
   <!--定义统一异常处理器-->
    <bean class="exception.CustomExceptionResolver"></bean>
```

##### 6.1.2.3统一异常处理逻辑
根据不同的异常类型进行异常处理。

系统自定义的异常类是CustomException ，在controller方法中、service方法中手动抛出此类异常。

针对系统自定义的CustomException异常，就可以直接从异常类中获取异常信息，将异常处理在错误页面展示。
针对非CustomException异常，对这类重新构造成一个CustomException，异常信息为“未知错误”，此类错误需要在系统测试阶段去排除。

在统一异常处理器CustomExceptionResolver中实现上边的逻辑。在CustomExceptionResolver.java中加入如下内容实现上述逻辑:
```java
   //输出异常
        ex.printStackTrace();


        //统一异常处理代码
        //针对系统自定义的CustomException异常，就可以直接从一场中获取一场信息，将异常处理在错误页面展示
        //异常信息
        String message=null;
        CustomException customException=null;
        //如果ex是系统自定义的异常，我们就直接取出异常信息
        if (ex instanceof CustomException)
        {
            customException= (CustomException) ex;
        }else {
            customException=new CustomException("未知错误");
        }

        //错误信息
        message=customException.getMessage();

        request.setAttribute("message",message);


        try {
            //转向到错误页面
            request.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(request,response);
        } catch (ServletException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ModelAndView();
    }
```

##### 6.1.2.4测试抛出异常由统一异常处理器捕获

可以在controller方法、service方法、dao实现类中抛出异常，要求dao、service、controller遇到异常全部向上抛出异常，方法向 上抛出异常throws Exception。

在ItemsServiceImpl.java的findItemsById方法中添加如下内容:
```java
    @Override
    public ItemsCustom findItemsById(int id) throws Exception {


        Items items=itemsMapper.selectByPrimaryKey(id);

        //如果查询的商品信息为空，抛出系统自定义的异常
        if (items==null)
        {
            throw new CustomException("修改商品信息不存在");
        }

        //在这里以后随着需求的变化，需要查询商品的其它相关信息，返回到controller
        //所以这个时候用到扩展类更好，如下
        ItemsCustom itemsCustom=new ItemsCustom();
        //将items的属性拷贝到itemsCustom
        BeanUtils.copyProperties(items,itemsCustom);

        return itemsCustom;
    }
```

图解:  

![](http://od2xrf8gr.bkt.clouddn.com/%E5%B1%8F%E5%B9%95%E5%BF%AB%E7%85%A7%202016-11-19%20%E4%B8%8B%E5%8D%884.56.44.png)  

测试:运行程序，在流浪器中我们输入`http://localhost:8080/SpringMvcMybatis/items/editItems.action?id=111`，由于没有哪个商品信息的id为111，所以出现异常，一旦出现异常，系统会立马走向统一异常处理器，执行统一异常处理器中的程序。

## 7.RESTful支持
### 7.1什么RESTful
RESTful是一种软件开发理念，RESTful对http进行非常好的诠释。RESTful即Representational State Transfer的缩写。关于RESTful我建议大家去看看这篇文章[理解RESTful架构--阮一峰的网络日志](http://www.ruanyifeng.com/blog/2011/09/restful.html)

### 7.2url的RESTful实现
非RESTful的http的url:`http://localhost:8080/items/editItems.action?id=1&....`  

RESTful的url是简洁的:`http:// localhost:8080/items/editItems/1`  

参数通过url传递，rest接口返回json数据

#### 7.2.1需求
根据id查看商品信息，商品信息查看的连接使用RESTful方式实现，商品信息以json返回。

#### 7.2.2第一步更改DispatcherServlet配置
在web.xml中再添加一个前端控制器:
```xml
    <!--RESTful的配置-->
    <servlet>
        <servlet-name>springmvc_rest</servlet-name>
        <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
        <!--加载springmvc配置文件-->
        <init-param>
            <param-name>contextConfigLocation</param-name>
            <!--配置文件的地址
            如果不配置contextConfigLocation，默认查找的配置文件名称是classpath下的:servlet名称+"-servlet.xml"即springmvc-servlet.xml-->
            <param-value>classpath:config/spring/springmvc.xml</param-value>
        </init-param>
    </servlet>
    <servlet-mapping>
        <servlet-name>springmvc_rest</servlet-name>
        <!--可以配置/：此工程所有的请求全部由springmvc解析，此种方式可以实现RESTful方式，需要特殊处理对静态文件的解析不能由springmvc解析
        可以配置*.do或者*.action,所有请求的url扩展名为.do或.action由springmvc解析，此中方法常用
        不可以配置/*,如果配置/*,返回jsp也由springmvc解析，这是不对的-->

        <!--reft方式配置为/-->
        <url-pattern>/</url-pattern>
    </servlet-mapping>
```

#### 7.2.3第二步参数通过url传递
在controller中添加一个方法:
```java
 //更具商品id查看商品信息rest接口
    //@requestMapping中指定restful方式的url中的参数，参数需要用{}包起来
    //@PathVariable将url中的参数和形参进行绑定
    @RequestMapping("/viewItems/{id}")
    public @ResponseBody ItemsCustom viewItems(@PathVariable("id") Integer id) throws Exception
    {
        //调用service查询商品的信息
        ItemsCustom itemsCustom=itemsService.findItemsById(id);


        return itemsCustom;
    }
```

此时我们便可以在浏览器中输入`http://localhost:8080/SpringMvcMybatis/items/viewItems/1`来查看商品信息。  

但是此时会有个bug，我们在web.xml中将访问所有路径的方式设置为:`<url-pattern>/</url-pattern>`即访问只要是该路径下的资源时都会经过前端控制器，这是不对的。当我们访问web包下的静态资源时也会经过前端控制器由springmvc解析，这当然是不正确的。所以我们需要设置静态资源的解析，在springmvc.xml中添加如下配置信息:
```xml
   <mvc:resources location="/js/" mapping="/js/**"/>
```
表示访问web/js/包下的静态资源时不会被前端控制器拦截也就不会被springmvc.xml解析。此时便可以正常访问静态资源。


## 8.springmvc拦截器

### 8.1拦截器的应用场合
用户请求到DispatherServlet中，DispatherServlet调用HandlerMapping查找Handler，HandlerMapping返回一个拦截的链儿（多个拦截），springmvc中的拦截器是通过HandlerMapping发起的。  

在企业开发，使用拦截器实现用户认证（用户登陆后进行身份校验拦截），用户权限拦截。  

### 8.2springmvc拦截器方法
定义一个拦截器HandlerInterceptor1.java实现HandlerInterceptor接口:
```java
public class HandlerInterceptor1 implements HandlerInterceptor
{
    //在执行handler之前来执行的
    //用于用户认证校验、用户权限校验
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		 //返回false表示进行拦截，不继续执行handler。返回true表示不进行拦截，继续执行handler
        return false;
    }


    //在执行handerl但是返回modelandview之前来执行
    //如果需要向页面提供一些公用的数据或配置一些视图信息，使用此方法实现从modelAndView入手
    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) throws Exception {

    }


    //执行handler之后执行此方法
    //做系统统一异常处理，进行方法执行性能监控，在prehandler中设置一个时间点，在afterCompletion设置一个时间点，两个时间点的差就是执行时长
    //实现系统统一日志记录
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) throws Exception {


    }
}
```

### 8.3测试拦截器
再定义一个拦截器HandlerInterceptor2.java，代码如下:
```java
public class HandlerInterceptor2 implements HandlerInterceptor
{
    //在执行handler之前来执行的
    //用于用户认证校验、用户权限校验
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {


        System.out.println("HandlerInterceptor2....postHandle");

        //返回false表示拦截不继续执行handler，返回true表示放行
        return false;
    }


    //在执行handerl但是返回modelandview之前来执行
    //如果需要向页面提供一些公用的数据或配置一些视图信息，使用此方法实现从modelAndView入手
    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) throws Exception {

        System.out.println("HandlerInterceptor2....postHandler");

    }


    //执行handler之后执行此方法
    //做系统统一异常处理，进行方法执行性能监控，在prehandler中设置一个时间点，在afterCompletion设置一个时间点，两个时间点的差就是执行时长
    //实现系统统一日志记录
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) throws Exception {

        System.out.println("HandlerInterceptor2....afterCompletion");

    }
}
```  

#### 8.3.1配置拦截器
配置全局的拦截器，DispatcherServlet将配置的全局拦截器加载到所有的HandlerMapping。在springmvc.xml中配置:
```xml
    <!--拦截器 -->
    <mvc:interceptors>
        <!--多个拦截器,顺序执行 --> 
        <mvc:interceptor>
            <mvc:mapping path="/**"/>
            <bean class="controller.interceptor.HandlerInterceptor1"></bean>
        </mvc:interceptor>
        <mvc:interceptor>
            <mvc:mapping path="/**"/>
            <bean class="controller.interceptor.HandlerInterceptor2"></bean>
        </mvc:interceptor>

    </mvc:interceptors>
```

#### 8.3.2测试1:拦截器1与2都放行
测试结果为:
```java
HandlerInterceptor1...preHandle
HandlerInterceptor2...preHandle

HandlerInterceptor2...postHandle
HandlerInterceptor1...postHandle

HandlerInterceptor2...afterCompletion
HandlerInterceptor1...afterCompletion
```

总结:执行preHandle是顺序执行。执行postHandle、afterCompletion是倒序执行。  

#### 8.3.3测试2:拦截器1放行，2不放行
此时访问界面，界面是空白的但是不会出现报错信息,控制台打印测试结果:
```java
HandlerInterceptor1...preHandle
HandlerInterceptor2...preHandle
HandlerInterceptor1...afterCompletion
```  

总结:如果preHandle不放行，postHandle、afterCompletion都不执行。只要有一个拦截器不放行，controller不能执行完成。  


#### 8.3.3测试2:拦截器1和2都不放行
测试结果:
```java
HandlerInterceptor1...preHandle
```
将这个测试结果和测试2结果进行比较，该测试结果只有拦截器1的preHandle方法执行而2的prehandle方法没有执行；而测试2结果拦截器1和2的preHandle都执行了，说明只有前边的拦截器放行时后面拦截器的preHandle方法才会执行。  


### 8.4拦截器的应用
需求:用户访问系统的资源(url)，如果用户没有进行身份认证，进行拦截，系统跳转登陆页面，如果用户已经认证通过，用户可以继续访问系统的资源。  

#### 8.4.1用户登录及退出功能开发
新建一个controller，LoginController.java,代码如下:
```java
@Controller
public class LoginController
{

    //用户登陆提交方法
    @RequestMapping("/login")
    public String login(HttpSession session,String usercode, String password) throws Exception
    {
        //调用service校验用户帐号和密码的正确性
        //这个东西我们讲shiro的时候再写



        //如果service校验通过，将用户身份记录到session
        session.setAttribute("usercode",usercode);

        //重定向到商品查询页面
        return "redirect:/items/queryItems.action";
    }

    //用户退出
    @RequestMapping("/logout")
    public String logout(HttpSession session) throws Exception
    {
        //session失效
        session.invalidate();

        //重定向到商品查询页面
        return "redirect:/items/queryItems.action";
    }
}
```

#### 8.4.2用户身份认证校验拦截器

拦截实现的思路:  

![](http://od2xrf8gr.bkt.clouddn.com/%E5%B1%8F%E5%B9%95%E5%BF%AB%E7%85%A7%202016-11-19%20%E4%B8%8B%E5%8D%885.29.43.png)

#### 8.4.3拦截器的编写
新建一个拦截器LoginInterceptor.java,代码如下:
```java
public class LoginInterceptor implements HandlerInterceptor
{
    //在执行handler之前来执行的
    //用于用户认证校验、用户权限校验
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String url=request.getRequestURI();

        //判断是否是公开地址
        //世纪开发中需要将公开地址配置在配置文件中

        if (url.indexOf("login.action")>=0)
        {
            //如果是公开地址则放行
            return true;
        }

        //判断用户身份在session中是否存在
        HttpSession session=request.getSession();
        String usercode= (String) session.getAttribute("usercode");

        //如果用户身份在session中存在则放行
        if (usercode!=null)
        {
            return true;
        }

        //执行到这里就拦截，跳转到登陆页面，用户进行登陆
        request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request,response);

        return false;
    }


    //在执行handerl但是返回modelandview之前来执行
    //如果需要向页面提供一些公用的数据或配置一些视图信息，使用此方法实现从modelAndView入手
    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) throws Exception {

        System.out.println("HandlerInterceptor1....postHandle");
    }


    //执行handler之后执行此方法
    //做系统统一异常处理，进行方法执行性能监控，在prehandler中设置一个时间点，在afterCompletion设置一个时间点，两个时间点的差就是执行时长
    //实现系统统一日志记录
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) throws Exception {

        System.out.println("HandlerInterceptor1....afterCompletion");

    }
}
```

然后在springmvc.xml中配置该拦截器，需要注视掉我们之前配置的拦截器:
```xml
 <!--拦截器 -->
    <mvc:interceptors>
        <!--多个拦截器,顺序执行 -->
        <!--<mvc:interceptor>-->
            <!--<mvc:mapping path="/**"/>-->
            <!--<bean class="controller.interceptor.HandlerInterceptor1"></bean>-->
        <!--</mvc:interceptor>-->
        <!--<mvc:interceptor>-->
            <!--<mvc:mapping path="/**"/>-->
            <!--<bean class="controller.interceptor.HandlerInterceptor2"></bean>-->
        <!--</mvc:interceptor>-->


        <mvc:interceptor>
            <!--/**可以拦截路径不管有多少层-->
            <mvc:mapping path="/**"/>
            <bean class="controller.interceptor.LoginInterceptor"></bean>
        </mvc:interceptor>
    </mvc:interceptors>
```

然后运行程序，当用户访问的不是公开url且用户没有登录时，该拦截器就会将用户的请求拦截并使用户跳转到登录页面进行登录。

## 9.联系

  If you have some questions after you see this article,you can tell your doubts in the comments area or you can find some info by  clicking these links.


- [Blog@codingXiaxw's blog](http://codingxiaxw.cn)

- [Weibo@codingXiaxw](http://weibo.com/u/5023661572?from=hissimilar_home&refer_flag=1005050003_)

- [Zhihu@codingXiaxw](http://www.zhihu.com/people/e9f78fa34b8002652811ac348da3f671)  
- [Github@codingXiaxw](https://github.com/codingXiaxw)