package com.drillgon.example.blocks.generic;

import com.drillgon.example.blocks.ModBlocks;
import com.drillgon.example.tileentity.TileEntityTestRender;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockTestRender extends BlockContainer {

	public BlockTestRender(Material materialIn, String s) {
		super(materialIn);
		this.setUnlocalizedName(s);
		this.setRegistryName(s);
		
		ModBlocks.ALL_BLOCKS.add(this);
	}

	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) {
		return new TileEntityTestRender();
	}

}
