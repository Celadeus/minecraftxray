package com.plusminus.craft;

import com.plusminus.craft.WorldInfo;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.LineNumberReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.zip.ZipFile;
import java.util.zip.ZipException;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import sun.security.action.GetBooleanAction;

/***
 * Utility class which has convenience methods to access the
 * files of the current minecraft installation
 * @author Vincent Vollers
 */
public class MineCraftEnvironment {
	public static enum OS {XP, Vista, MacOS, Linux, NotSupported};
	public static OS os; 
	public static File baseDir;
	
	static {
		String os = System.getProperty( "os.name" );
		HashMap<String,OS> osData = new HashMap<String,OS>();
		/*  Full list of os.name strings
		    AIX
			Digital Unix
			FreeBSD
			HP UX
			Irix
			Linux
			Mac OS
			Mac OS X
			MPE/iX
			Netware 4.11
			OS/2
			Solaris
			Windows 2000
			Windows 7
			Windows 95
			Windows 98
			Windows NT
			Windows Vista
			Windows XP
		 */
		osData.put("FreeBSD", 	OS.Linux);
		osData.put("HP UX", 	OS.Linux);
		osData.put("Linux", 	OS.Linux);
		osData.put("Mac OS", 	OS.MacOS);
		osData.put("Mac OS X", 	OS.MacOS);
		osData.put("Windows", 	OS.XP);
		osData.put("Windows 7", OS.Vista);
		osData.put("Windows XP", OS.XP);
		osData.put("Windows 2003", OS.XP);
		osData.put("Windows 2000", OS.XP);
		osData.put("Windows Vista", OS.Vista);
		
		if(!osData.containsKey(os)) {
			MineCraftEnvironment.os = OS.NotSupported;
		} else {
			MineCraftEnvironment.os = osData.get(os);
		}
		
		switch(MineCraftEnvironment.os) {
			case Vista:
				MineCraftEnvironment.baseDir = new File(System.getenv("APPDATA"), ".minecraft");
				break;
			case Linux:
				MineCraftEnvironment.baseDir = new File(System.getProperty("user.home"), ".minecraft"); // untested
				break;
			case XP:
				MineCraftEnvironment.baseDir = new File(System.getenv("APPDATA"), ".minecraft"); // untested
				break;
			case MacOS:
				// damn macs ;p
				File dotMinecraftEnv = new File(System.getProperty("user.home"), "Library/Application Support/.minecraft");
				if(dotMinecraftEnv.exists()) {
					MineCraftEnvironment.baseDir = dotMinecraftEnv;
				} else {
					MineCraftEnvironment.baseDir = new File(System.getProperty("user.home"), "Library/Application Support/minecraft"); // untested
				}
				break;
			default:
				MineCraftEnvironment.baseDir = null;
		}
		System.out.println(MineCraftEnvironment.baseDir.getAbsolutePath());
	}
	
	/***
	 * Returns a list of WorldInfo objects, corresponding to available worlds
	 * @return
	 */
	public static ArrayList<WorldInfo> getAvailableWorlds() {
		ArrayList<WorldInfo> worlds = new ArrayList<WorldInfo>();
		for(int i=0;i<10;i++) {
			File worldDir = getWorldDirectory(i);
			if(worldDir.exists() && worldDir.canRead()) {
				try
				{
					// First snatch up the overworld
					WorldInfo info = new WorldInfo(worldDir.getCanonicalPath(), i);
					worlds.add(info);
					
					// Now see if there's an associated Nether world we can add.
					WorldInfo netherinfo = info.getNetherInfo();
					if (netherinfo != null)
					{
						worlds.add(netherinfo);
					}
				}
				catch (IOException e)
				{
					// Nothing; guess we'll ignore it.
				}
			}
		}
		return worlds;
	}
	
	/***
	 * Returns a file handle to a chunk file in a world
	 * @param world
	 * @param x
	 * @param z
	 * @return
	 */
	public static File getChunkFile(WorldInfo world, int x, int z) {
		int xx = x % 64;
		if(xx<0) xx = 64+xx;
		int zz = z % 64;
		if(zz<0) zz = 64+zz;
		String firstFolder 		= Integer.toString(xx, 36);
		String secondFolder 	= Integer.toString(zz, 36);
		String filename 		= "c." + Integer.toString(x, 36) + "." + Integer.toString(z, 36) + ".dat";
		return new File(world.getBasePath(), firstFolder + "/" + secondFolder + "/" + filename);
	}
	

	/***
	 * Returns a file handle to a base world directory
	 * @param world
	 * @return
	 */
	public static File getWorldDirectory(int world) {
		return new File(baseDir, "saves/World" + world);
	}
	
	/***
	 * Returns a file handle to the base minecraft directory
	 * @return
	 */
	public static File getMinecraftDirectory() {
		return MineCraftEnvironment.baseDir;
	}
	
	/***
	 * Returns a stream to an arbitrary file either from the main jar, or from the user-specified
	 * texture pack.
	 * 
	 * @return
	 */
	public static InputStream getMinecraftTexturepackData(String filename) {
		// First check the options.txt file to see if we should be using the defined
		// texture pack.
		File optionsFile = new File(baseDir, "options.txt");
		String texturepack = null;
		if (optionsFile.exists())
		{
			LineNumberReader reader = null;
			try
			{
				reader = new LineNumberReader(new FileReader(optionsFile));
				String line = null;
				String[] parts;
				while ((line = reader.readLine()) != null)
				{
					parts = line.split(":", 2);
					if (parts.length == 2)
					{
						if (parts[0].equalsIgnoreCase("skin"))
						{
							if (parts[1].equalsIgnoreCase("Default"))
							{
								// Default skin, just break and 
								break;
							}
							else
							{
								// Use the specified texture pack
								texturepack = parts[1];
								break;
							}
						}
					}
				}
			}
			catch (FileNotFoundException e)
			{
				// Just ignore it and load the default terrain.png
			}
			catch (IOException e)
			{
				// Ditto, just ignore
			}
			if (reader != null)
			{
				try
				{
					reader.close();
				}
				catch (IOException e)
				{
					// do nothing
				}
			}
		}
		
		// Attempt to load in the texture pack
		if (texturepack != null)
		{
			File packFile = new File(baseDir, "texturepacks/" + texturepack);
			if (packFile.exists())
			{
				ZipFile zf = null;
				try
				{
					zf = new ZipFile(packFile);
					ZipEntry entry = zf.getEntry(filename);
					if (entry != null)
					{
						return zf.getInputStream(entry);
					}
				}
				catch (ZipException e)
				{
					// Do nothing.
				}
				catch (IOException e)
				{
					// Do nothing
				}
				if (zf != null)
				{
					try
					{
						zf.close();
					}
					catch (IOException e)
					{
						// do nothing
					}
				}
			}
		}
		
		// If we got here, just do what we've always done.
		return getMinecraftFile(filename);
	}
	
	/***
	 * Returns a stream to the texture data (overrides in the directory are handled)
	 * @return
	 */
	public static InputStream getMinecraftTextureData() {
		return getMinecraftTexturepackData("terrain.png");
	}
	
	/**
	 * Returns a stream to the painting data
	 * @return
	 */
	public static InputStream getMinecraftPaintingData() {
		return getMinecraftTexturepackData("art/kz.png");
	}
	
	/***
	 * Returns a stream to the font data (overrides in the directory are handled)
	 * @return
	 */
	public static InputStream getMinecraftFontData() {
		return getMinecraftFile("default.png");
	}
	
	/***
	 * Attempts to create a bufferedImage for a stream
	 * @param i
	 * @return
	 */
	private static BufferedImage buildImageFromInput(InputStream i) {
		try {
			return ImageIO.read(i);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Attempts to create a bufferedImage containing our painting sheet
	 * @return
	 */
	public static BufferedImage getMinecraftPaintings() {
		return buildImageFromInput(getMinecraftPaintingData());
	}
	
	/***
	 * Attempts to create a bufferedImage containing the texture sprite sheet
	 * @return
	 */
	public static BufferedImage getMinecraftTexture() {
		BufferedImage bi = buildImageFromInput(getMinecraftTextureData());
		
		// Figure out our square size, and then check to see if the grass tile is
		// grayscale or not; we do this by examining the middle row of pixels.  If
		// it *is* grayscale, then colorize it. 
		int square_width = bi.getWidth()/16;
		int[] pixels = new int[square_width];
		int i;
		int r, g, b;
		boolean grayscale = true;
		bi.getRGB(0, square_width/2, square_width, 1, pixels, 0, square_width);
		for (i=0; i<square_width; i++)
		{
			//a = (pixels[i] & 0xFF000000) >> 24;
			r = (pixels[i] & 0x00FF0000) >> 16;
			g = (pixels[i] & 0x0000FF00) >> 8;
			b = (pixels[i] & 0x000000FF);
			//System.out.println("Pixel " + i + ": " + r + ", " + g + ", " + b + ", " + a);
			if (g > r || g > b)
			{
				grayscale = false;
				break;
			}
		}
		
		// Now do the coloring if we have to.
		if (grayscale)
		{	
			Graphics2D g2d = bi.createGraphics();
			AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.3f);
			
			// First greenify grass
			Rectangle rect = new Rectangle(0, 0, square_width, square_width);
			g2d.setComposite(ac);
			g2d.setColor(Color.green);
			g2d.fill(rect);
			g2d.drawImage(bi, null, 0, 0);
			
			// Now greenify leaves
			rect = new Rectangle(4*square_width, 3*square_width, 2*square_width, square_width);
			g2d.setComposite(ac);
			g2d.setColor(Color.green);
			g2d.fill(rect);
			g2d.drawImage(bi, null, 0, 0);
		}
		return bi;
	}
	
	/***
	 * Attempts to create a bufferedImage containing the font
	 * @return
	 */
	public static BufferedImage getMinecraftFont() {
		return buildImageFromInput(getMinecraftFontData());
	}
	
	/***
	 * Creates an Inputstream to a file in the bin/ directory in the minecraft directory.
	 * This handles overrides. If a file exists in the bin/ directory it will load that.
	 * Otherwise it will look in the .jar file of minecraft
	 * @param fileName
	 * @return
	 */
	public static InputStream getMinecraftFile(String fileName){
		File overrideFile = new File(baseDir, "bin/" + fileName);

		if(overrideFile.exists()) {
			try {
				return new FileInputStream(overrideFile);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		}
		
		File minecraftDataFile = new File(baseDir, "bin/minecraft.jar");
		if(!minecraftDataFile.exists()) {
			return null;
		}
		try {
			JarFile jf = new JarFile(minecraftDataFile);
			ZipEntry zipEntry = jf.getEntry(fileName);
			if(zipEntry == null) {
				return null;
			}
			return jf.getInputStream(zipEntry);
		} catch (IOException e) {
			System.out.println(e.toString());
			return null;
		}
	}
}
