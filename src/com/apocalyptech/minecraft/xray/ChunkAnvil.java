/**
 * Copyright (c) 2010-2012, Vincent Vollers and Christopher J. Kucera
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Minecraft X-Ray team nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL VINCENT VOLLERS OR CJ KUCERA BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.apocalyptech.minecraft.xray;

import java.lang.Math;
import java.util.Map;
import java.util.Random;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.PatternSyntaxException;

import org.lwjgl.opengl.GL11;

import com.apocalyptech.minecraft.xray.dtf.ShortArrayTag;
import com.apocalyptech.minecraft.xray.dtf.ByteArrayTag;
import com.apocalyptech.minecraft.xray.dtf.CompoundTag;
import com.apocalyptech.minecraft.xray.dtf.StringTag;
import com.apocalyptech.minecraft.xray.dtf.ListTag;
import com.apocalyptech.minecraft.xray.dtf.ByteTag;
import com.apocalyptech.minecraft.xray.dtf.IntTag;
import com.apocalyptech.minecraft.xray.dtf.Tag;

import static com.apocalyptech.minecraft.xray.MinecraftConstants.*;

/**
 * A new-style "Anvil" chunk.  Similar to the original, except that the
 * data is split up into 16x16x16 "Sections"
 */
public class ChunkAnvil extends Chunk {

	private HashMap<Byte, ShortArrayTag> blockData;
	private HashMap<Byte, ByteArrayTag> mapData;
	private HashMap<Byte, Boolean> availableSections;
	private ArrayList<Byte> availableSectionsList;

	private byte lSection;
	
	public ChunkAnvil(MinecraftLevel level, Tag data) {

		super(level, data);

		blockData = new HashMap<Byte, ShortArrayTag>();
		mapData = new HashMap<Byte, ByteArrayTag>();
		availableSections = new HashMap<Byte, Boolean>();
		availableSectionsList = new ArrayList<Byte>();
		
		ListTag sectionsTag = (ListTag) this.levelTag.getTagWithName("Sections");
		for (Tag sectionTagTemp : sectionsTag.value)
		{
			CompoundTag sectionTag = (CompoundTag) sectionTagTemp;
			ByteTag sectionNumTag = (ByteTag) sectionTag.getTagWithName("Y");
			byte section = sectionNumTag.value;
			availableSections.put(section, true);
			availableSectionsList.add(section);
			blockData.put(section, (ShortArrayTag) sectionTag.getTagWithName("Blocks"));
			mapData.put(section, (ByteArrayTag) sectionTag.getTagWithName("Data"));
		}

		// Make sure our list of available sections is ordered
		Collections.sort(availableSectionsList);

		this.finishConstructor();
	}
	
	/**
	 * Will return an array of values which are suitable for feeding into a
	 * minimap.
	 */
	public short[] getMinimapValues(boolean nether)
	{
		return new short[0];
	}

	/**
	 * Gets the Block ID of the block immediately to the west.  This might
	 * load in the adjacent chunk, if needed.  Will return -1 if that adjacent
	 * chunk can't be found.
	 */
	protected short getAdjWestBlockId(int x, int y, int z, int blockOffset)
	{
		if (x > 0)
		{
			byte section = (byte)(y/16);
			if (this.blockData.containsKey(section))
			{
				return blockData.get(section).value[blockOffset-1];
			}
			else
			{
				return -1;
			}
		}
		else
		{
			Chunk otherChunk = level.getChunk(this.x-1, this.z);
			if (otherChunk == null)
			{
				return -1;
			}
			else
			{
				return otherChunk.getBlock(15, y, z);
			}
		}
	}

	/**
	 * Gets the Block ID of the block immediately to the east.  This might
	 * load in the adjacent chunk, if needed.  Will return -1 if that adjacent
	 * chunk can't be found.
	 */
	protected short getAdjEastBlockId(int x, int y, int z, int blockOffset)
	{
		if (x < 15)
		{
			byte section = (byte)(y/16);
			if (this.blockData.containsKey(section))
			{
				return blockData.get(section).value[blockOffset+1];
			}
			else
			{
				return -1;
			}
		}
		else
		{
			Chunk otherChunk = level.getChunk(this.x+1, this.z);
			if (otherChunk == null)
			{
				return -1;
			}
			else
			{
				return otherChunk.getBlock(0, y, z);
			}
		}
	}

	/**
	 * Gets the Block ID of the block immediately to the south.  This might
	 * load in the adjacent chunk, if needed.  Will return -1 if that adjacent
	 * chunk can't be found.
	 */
	protected short getAdjNorthBlockId(int x, int y, int z, int blockOffset)
	{
		if (z > 0)
		{
			byte section = (byte)(y/16);
			if (blockData.containsKey(section))
			{
				return blockData.get(section).value[blockOffset-16];
			}
			else
			{
				return -1;
			}
		}
		else
		{
			Chunk otherChunk = level.getChunk(this.x, this.z-1);
			if (otherChunk == null)
			{
				return -1;
			}
			else
			{
				return otherChunk.getBlock(x, y, 15);
			}
		}
	}

	/**
	 * Gets the Block ID of the block immediately to the north.  This might
	 * load in the adjacent chunk, if needed.  Will return -1 if that adjacent
	 * chunk can't be found.
	 */
	protected short getAdjSouthBlockId(int x, int y, int z, int blockOffset)
	{
		if (z < 15)
		{
			byte section = (byte)(y/16);
			if (blockData.containsKey(section))
			{
				return blockData.get(section).value[blockOffset+16];
			}
			else
			{
				return -1;
			}
		}
		else
		{
			Chunk otherChunk = level.getChunk(this.x, this.z+1);
			if (otherChunk == null)
			{
				return -1;
			}
			else
			{
				return otherChunk.getBlock(x, y, 0);
			}
		}
	}

	/**
	 * Gets the Block ID of the block immediately up.
	 * Will return -1 if we're already at the top
	 */
	protected short getAdjUpBlockId(int x, int y, int z, int blockOffset)
	{
		byte section = (byte)(y/16);
		if ((y % 16) == 15)
		{
			if (blockData.containsKey((byte)(section + 1)))
			{
				return blockData.get((byte)(section+1)).value[x + (z*16)];
			}
			else
			{
				return 0;
			}
		}
		else
		{
			return blockData.get(section).value[blockOffset+256];
		}
	}

	/**
	 * Gets the Block ID of the block immediately down.
	 * Will return -1 if we're already at the bottom
	 */
	protected short getAdjDownBlockId(int x, int y, int z, int blockOffset)
	{
		byte section = (byte)(y/16);
		if ((y % 16) == 0)
		{
			if (blockData.containsKey((byte)(section - 1)))
			{
				return blockData.get((byte)(section-1)).value[3840 + x + (16*z)];
			}
			else
			{
				return 0;
			}
		}
		else
		{
			return blockData.get(section).value[blockOffset-256];
		}
	}
	
	/**
	 * Gets the block ID at the specified coordinate in the chunk.  This is
	 * only really used in the getAdj*BlockId() methods.
	 */
	public short getBlock(int x, int y, int z) {
		byte section = (byte)(y/16);
		if (blockData.containsKey(section))
		{
			return blockData.get(section).value[((y % 16) * 256) + (z * 16) + x];
		}
		else
		{
			return 0;
		}
	}

	/**
	 * Gets the block data at the specified coordinates.
	 */
	public byte getData(int x, int y, int z) {
		byte section = (byte)(y/16);
		if (mapData.containsKey(section))
		{
			int offset = ((y%16)*256) + (z * 16) + x;
			int halfOffset = offset / 2;
			if(offset % 2 == 0) {
				return (byte) (mapData.get(section).value[halfOffset] & 0xF);
			} else {
				// We shouldn't have to &0xF here, but if we don't the value
				// returned could be negative, even though that would be silly.
				return (byte) ((mapData.get(section).value[halfOffset] >> 4) & 0xF);
			}
		}
		else
		{
			return (byte)0;
		}
	}
	
	/**
	 * Tests if the given source block has a torch nearby.  This is, I'm willing
	 * to bet, the least efficient way possible of doing this.  It turns out that
	 * despite that, it doesn't really have a noticeable impact on performance,
	 * which is why it remains in here, but perhaps one day I'll rewrite this
	 * stuff to be less stupid.  The one upside to doing it like this is that
	 * we're not using any extra memory storing data about which block should be
	 * highlighted...
	 *
	 * TODO: Should implement this in Chunk, not here.
	 * 
	 * @param sx
	 * @param sy
	 * @param sz
	 * @return
	 */
	public boolean hasAdjacentTorch(int sx, int sy, int sz)
	{
		int distance = 3;
		int x, y, z;
		int min_x = sx-distance;
		int max_x = sx+distance;
		int min_z = sz-distance;
		int max_z = sz+distance;
		int min_y = Math.max(0, sy-distance);
		int max_y = Math.min(127, sy+distance);
		Chunk otherChunk;
		int cx, cz;
		int tx, tz;
		for (x = min_x; x<=max_x; x++)
		{
			for (y = min_y; y<=max_y; y++)
			{
				for (z = min_z; z<=max_z; z++)
				{
					otherChunk = null;
					if (x < 0)
					{
						cx = this.x-1;
						tx = 16+x;
					}
					else if (x > 15)
					{
						cx = this.x+1;
						tx = x-16;
					}
					else
					{
						cx = this.x;
						tx = x;
					}

					if (z < 0)
					{
						cz = this.z-1;
						tz = 16+z;
					}
					else if (z > 15)
					{
						cz = this.z+1;
						tz = z-16;
					}
					else
					{
						cz = this.z;
						tz = z;
					}
					
					if (cx != this.x || cz != this.z)
					{
						otherChunk = level.getChunk(cx, cz);
						if (otherChunk == null)
						{
							continue;
						}
						/* TODO: yeah
						else if (exploredBlocks.containsKey(otherChunk.blockData.value[(tz*128)+(tx*128*16)+y]))
						{
							return true;
						}
						*/
					}
					else
					{
						/* TODO: yeah
						if (exploredBlocks.containsKey(blockData.value[(z*128)+(x*128*16)+y]))
						{
							return true;
						}
						*/
					}
				}
			}
		}
		return false;
	}

	/**
	 * Rewind our loop
	 */
	protected void rewindLoop()
	{
		super.rewindLoop();
		this.lSection = -1;
	}

	/**
	 * Advances our block loop
	 */
	protected short nextBlock()
	{
		boolean advance_to_next_section = false;
		boolean found_next_section = false;
		this.lOffset = ((this.lOffset+1) % 4096);
		if (this.lOffset == 0)
		{
			advance_to_next_section = true;
			for (byte section : this.availableSectionsList)
			{
				if (section > this.lSection)
				{
					found_next_section = true;
					this.lSection = section;
					break;
				}
			}
		}
		if (advance_to_next_section && !found_next_section)
		{
			return -2;
		}
		this.lx = this.lOffset % 16;
		this.lz = (this.lOffset / 16) % 16;
		this.ly = (this.lOffset / 256) + (16*this.lSection);

		return this.blockData.get(this.lSection).value[this.lOffset];
	}

}