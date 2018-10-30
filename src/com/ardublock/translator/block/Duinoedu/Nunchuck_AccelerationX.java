package com.ardublock.translator.block.Duinoedu;

import com.ardublock.translator.Translator;
import com.ardublock.translator.block.TranslatorBlock;
import com.ardublock.translator.block.exception.SocketNullException;
import com.ardublock.translator.block.exception.SubroutineNotDeclaredException;

public class Nunchuck_AccelerationX  extends TranslatorBlock {

	public Nunchuck_AccelerationX (Long blockId, Translator translator, String codePrefix, String codeSuffix, String label)
	{
		super(blockId, translator, codePrefix, codeSuffix, label);
	}
	
	//@Override
		public String toCode() throws SocketNullException, SubroutineNotDeclaredException
		{		
			translator.addHeaderFile("Wire.h");
			translator.addHeaderFile("Nunchuck.h");
			translator.addDefinitionCommand("Nunchuck monNunchuck;");
			translator.addDefinitionCommand("//libraries at http://duinoedu.com/dl/lib/grove/EDU_Nunchuck/");
			translator.addSetupCommand("monNunchuck.brancher();");
			
			return codePrefix +"monNunchuck.lireAccelerationX()" + codeSuffix;	
		}
}
