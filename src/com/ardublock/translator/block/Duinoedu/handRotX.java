package com.ardublock.translator.block.Duinoedu;

import com.ardublock.translator.Translator;
import com.ardublock.translator.block.TranslatorBlock;
import com.ardublock.translator.block.exception.SocketNullException;

public class handRotX extends TranslatorBlock
{

	public handRotX(Long blockId, Translator translator, String codePrefix, String codeSuffix, String label) {
		super(blockId, translator, codePrefix, codeSuffix, label);
	}

	@Override
	public String toCode() throws SocketNullException {
		translator.addHeaderFile("SerialPlus.h");
		translator.addDefinitionCommand("//libraries at http://duinoedu.com/dl/lib/autre/EDU_SerialPlus/");
		translator.addDefinitionCommand("SerialPlus monSerialLeap;");
		translator.addSetupCommand("monSerialLeap.branch(&Serial); \nmonSerialLeap.begin(9600);");
		//return codePrefix + "handRotX" + codeSuffix;
		return codePrefix + "monSerialLeap.readNbr(CANAL83)" + codeSuffix;
	}

}
