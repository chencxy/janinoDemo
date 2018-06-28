package com.cxy.demo.janinoDemo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import org.apache.commons.io.IOUtils;
import org.codehaus.janino.ClassBodyEvaluator;
import org.codehaus.janino.util.ResourceFinderClassLoader;
import org.codehaus.janino.util.resource.LazyMultiResourceFinder;
import org.codehaus.janino.util.resource.MapResourceFinder;
import org.codehaus.janino.util.resource.ResourceFinder;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

public class JaninoCompiler{
	
	private String libDir;
	
	private ClassBodyEvaluator localClassBodyEvaluator;

	public JaninoCompiler(String libDir) throws FileNotFoundException, IOException {
		super();
		this.libDir = libDir;
		this.buildEvaluator();
	}
	
	private void buildEvaluator() throws FileNotFoundException, IOException {
		List<ResourceFinder> localArrayList = new ArrayList<ResourceFinder>();
		File path = new File(libDir);
		if(path.isDirectory()){
			for(File f : path.listFiles()){
				if(!f.getName().endsWith(".jar"))
					continue;
				JarInputStream localJarInputStream = new JarInputStream(new FileInputStream(f));
				Map<String, byte[]> localHashMap2 = new HashMap<String, byte[]>();
			      for (ZipEntry localObject3 = localJarInputStream.getNextEntry(); localObject3 != null; localObject3 = localJarInputStream.getNextEntry())
			        if (!localObject3.isDirectory())
			        {
			          localHashMap2.put(localObject3.getName(), StreamUtils.copyToByteArray(localJarInputStream));
			        }
			      localJarInputStream.close();
			      localArrayList.add(new MapResourceFinder(localHashMap2));
			}
		}
		ResourceFinderClassLoader loader = new ResourceFinderClassLoader(new LazyMultiResourceFinder(localArrayList.iterator()), getClass().getClassLoader());
		
	    localClassBodyEvaluator = new ClassBodyEvaluator();
	    localClassBodyEvaluator.setParentClassLoader((ClassLoader)loader);
	}

	public Class<?> compile(String  scriptFile) throws Exception {
	    String source = IOUtils.toString(new FileInputStream(scriptFile));
		localClassBodyEvaluator.cook(source);
		
		Class<?> clazz = localClassBodyEvaluator.getClazz();
		return clazz;
	}
	
	/**
	 * copy from StreamUtils
	 */
	private static class StreamUtils{
		public static final int BUFFER_SIZE = 4096;
		
		public static int copy(InputStream in, OutputStream out) throws IOException {
			int byteCount = 0;
			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead = -1;
			while ((bytesRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, bytesRead);
				byteCount += bytesRead;
			}
			out.flush();
			return byteCount;
		}
		
		public static byte[] copyToByteArray(InputStream in) throws IOException {
			if (in == null) {
				return new byte[0];
			}

			ByteArrayOutputStream out = new ByteArrayOutputStream(BUFFER_SIZE);
			copy(in, out);
			return out.toByteArray();
		}
	}
	
	public static void main(String[] args) throws Exception {
		JaninoCompiler compiler = new JaninoCompiler("src/main/resources/libs");
		Class<?> clazz = compiler.compile(compiler.getClass().getResource("/sourceCode.jan").getPath());
		
		Method method = clazz.getMethod("test", new Class<?>[] {});
		Object object = method.invoke(null, new Object[] {});
		System.out.println(JSON.toJSONString(object, SerializerFeature.WriteClassName, SerializerFeature.PrettyFormat));
	}
}
