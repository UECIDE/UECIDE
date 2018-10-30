package com.ardublock.translator.block.Duinoedu;

import com.ardublock.translator.Translator;
import com.ardublock.translator.block.TranslatorBlock;
import com.ardublock.translator.block.exception.SocketNullException;
import com.ardublock.translator.block.exception.SubroutineNotDeclaredException;

public class Guino_Read  extends TranslatorBlock {

	public Guino_Read (Long blockId, Translator translator, String codePrefix, String codeSuffix, String label)
	{
		super(blockId, translator, codePrefix, codeSuffix, label);
	}
	
	//@Override
		public String toCode() throws SocketNullException, SubroutineNotDeclaredException
		{
			
			String Title;
			String Variable;
			String Min;
			String Max;
			TranslatorBlock translatorBlock = this.getRequiredTranslatorBlockAtSocket(0);
			Title = translatorBlock.toCode();
			translatorBlock = this.getRequiredTranslatorBlockAtSocket(1);
			Variable = translatorBlock.toCode();
			translatorBlock = this.getRequiredTranslatorBlockAtSocket(2);
			Min = translatorBlock.toCode();
			translatorBlock = this.getRequiredTranslatorBlockAtSocket(3);
			Max = translatorBlock.toCode();
			
			String internalVariableName = translator.getNumberVariable(Variable);
			if (internalVariableName == null)
			{
				internalVariableName =label.replace(" ","")+ translator.buildVariableName(Variable);
				translator.addNumberVariable(Variable, internalVariableName);
				translator.addDefinitionCommand("int " + internalVariableName + " = 0 ;");
//				translator.addSetupCommand(internalVariableName + " = 0;");
			}
			
			translator.addHeaderFile("EasyTransfer.h");
			translator.addHeaderFile("EEPROM.h");
			translator.addHeaderFile("Guino.h");
			translator.addDefinitionCommand("//libraries at http://duinoedu.com/dl/lib/dupont/EDU_Guino/");
			translator.addSetupCommand("GUINO_BRANCHER();");
			translator.addGuinoCommand("GUINO_AFFICHER_GRAPH("+Title+","+internalVariableName+","+Min+","+Max+");\nGUINO_AFFICHER_LIGNE(); ");
			translator.addGuinoCommand("GUINO_AFFICHER_POTENTIOMETRE("+Title+","+internalVariableName+","+Min+","+Max+");\nGUINO_AFFICHER_LIGNE(); ");
			
			
			String ret =  	internalVariableName+"="+Variable+";\n"+
							"GUINO_LIRE("+internalVariableName+");\n";
			return  ret ;
			
		}
}
