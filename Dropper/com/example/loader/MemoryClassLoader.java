package com.example.loader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

public class MemoryClassLoader extends ClassLoader {
  private final Map<String, byte[]> classData;
  
  private final Map<String, byte[]> resourceData;
  
  public MemoryClassLoader(Map<String, byte[]> classData, Map<String, byte[]> resourceData) {
    super(Thread.currentThread().getContextClassLoader());
    this.classData = classData;
    this.resourceData = resourceData;
  }
  
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    byte[] bytes = this.classData.get(name);
    if (bytes == null)
      throw new ClassNotFoundException(name); 
    return defineClass(name, bytes, 0, bytes.length);
  }
  
  public InputStream getResourceAsStream(String name) {
    byte[] data = this.resourceData.get(name);
    if (data != null)
      return new ByteArrayInputStream(data); 
    if (name.startsWith("/")) {
      data = this.resourceData.get(name.substring(1));
      if (data != null)
        return new ByteArrayInputStream(data); 
    } 
    return super.getResourceAsStream(name);
  }
}
