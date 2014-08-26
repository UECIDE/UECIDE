package org.uecide.plugin;

import org.uecide.*;
import org.uecide.debug.*;
import org.uecide.editors.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import say.swing.*;
import org.json.simple.*;
import java.beans.*;

import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.*;
import org.fife.ui.rsyntaxtextarea.folding.*;

public class ElfInspector extends Plugin {

    public RandomAccessFile elf;
    static String types[] = {
        "None", "Relocatable", "Executable", "Shared Object", "Core"
    };
    static String machines[] = {
        "None", "AT&T WE 32100", "SPARC", "Intel 80386", "Motorola 68000", "Motorola 88000", "Reserved", "Intel 80860", "MIPS I Architecture", "IBM System/370 Processor",
        "MIPS RS3000 Little-endian", "Reserved", "Reserved", "Reserved", "Reserved", "Hewlett-Packard PA-RISC", "Reserved", "Fujitsu VPP500", "Enhanced instruction set SPARC", "Intel 80960",
        "PowerPC", "64-bit PowerPC", "IBM System/390 Processor", "IBM SPU/SPC", "Reserved", "Reserved", "Reserved", "Reserved", "Reserved", "Reserved",
        "Reserved", "Reserved", "Reserved", "Reserved", "Reserved", "Reserved", "NEC V800", "Fujitsu FR20", "TRW RH-32", "Motorola RCE",
        "ARM 32-bit architecture (AARCH32)", "Digital Alpha", "Hitachi SH", "SPARC Version 9", "Siemens TriCore embedded processor", "Argonaut RISC Core, Argonaut Technologies Inc.", "Hitachi H8/300", "Hitachi H8/300H", "Hitachi H8S", "Hitachi H8/500",
        "Intel IA-64 processor architecture", "Stanford MIPS-X", "Motorola ColdFire", "Motorola M68HC12", "Fujitsu MMA Multimedia Accelerator", "Siemens PCP", "Sony nCPU embedded RISC processor", "Denso NDR1 microprocessor", "Motorola Star*Core processor", "Toyota ME16 processor",
        "STMicroelectronics ST100 processor", "Advanced Logic Corp. TinyJ embedded processor family", "AMD x86-64 architecture", "Sony DSP Processor", "Digital Equipment Corp. PDP-10", "Digital Equipment Corp. PDP-11", "Siemens FX66 microcontroller", "STMicroelectronics ST9+ 8/16 bit microcontroller", "STMicroelectronics ST7 8-bit microcontroller", "Motorola MC68HC16 Microcontroller",
        "Motorola MC68HC11 Microcontroller", "Motorola MC68HC08 Microcontroller", "Motorola MC68HC05 Microcontroller", "Silicon Graphics SVx", "STMicroelectronics ST19 8-bit microcontroller", "Digital VAX", "Axis Communications 32-bit embedded processor", "Infineon Technologies 32-bit embedded processor", "Element 14 64-bit DSP Processor", "LSI Logic 16-bit DSP Processor", 
        "Donald Knuth's educational 64-bit processor", "Harvard University machine-independent object files", "SiTera Prism", "Atmel AVR 8-bit microcontroller", "Fujitsu FR30", "Mitsubishi D10V", "Mitsubishi D30V", "NEC v850", "Mitsubishi M32R", "Matsushita MN10300",
        "Matsushita MN10200", "picoJava", "OpenRISC 32-bit embedded processor", "ARC International ARCompact processor (old spelling/synonym: EM_ARC_A5)", "Tensilica Xtensa Architecture", "Alphamosaic VideoCore processor", "Thompson Multimedia General Purpose Processor", "National Semiconductor 32000 series", "Tenor Network TPC processor", "Trebia SNP 1000 processor",
        "STMicroelectronics (www.st.com) ST200 microcontroller", "Ubicom IP2xxx microcontroller family", "MAX Processor", "National Semiconductor CompactRISC microprocessor", "Fujitsu F2MC16", "Texas Instruments embedded microcontroller msp430", "Analog Devices Blackfin (DSP) processor", "S1C33 Family of Seiko Epson processors", "Sharp embedded microprocessor", "Arca RISC Microprocessor",
        "Microprocessor series from PKU-Unity Ltd. and MPRC of Peking University", "eXcess: 16/32/64-bit configurable embedded CPU", "Icera Semiconductor Inc. Deep Execution Processor", "Altera Nios II soft-core processor", "National Semiconductor CompactRISC CRX microprocessor", "Motorola XGATE embedded processor", "Infineon C16x/XC16x processor", "Renesas M16C series microprocessors", "Microchip Technology dsPIC30F Digital Signal Controller", "Freescale Communication Engine RISC core",
        "Renesas M32C series microprocessors", "Reserved", "Reserved", "Reserved", "Reserved", "Reserved", "Reserved", "Reserved", "Reserved", "Reserved", 
        "Reserved", "Altium TSK3000 core", "Freescale RS08 embedded processor", "Analog Devices SHARC family of 32-bit DSP processors", "Cyan Technology eCOG2 microprocessor", "Sunplus S+core7 RISC processor", "New Japan Radio (NJR) 24-bit DSP Processor", "Broadcom VideoCore III processor", "RISC processor for Lattice FPGA architecture", "Seiko Epson C17 family",
        "The Texas Instruments TMS320C6000 DSP family", "The Texas Instruments TMS320C2000 DSP family", "The Texas Instruments TMS320C55x DSP family", "Reserved",  "Reserved",  "Reserved",  "Reserved",  "Reserved",  "Reserved",  "Reserved", 
        "Reserved", "Reserved", "Reserved", "Reserved", "Reserved", "Reserved", "Reserved", "Reserved", "Reserved", "Reserved", 
        "STMicroelectronics 64bit VLIW Data Signal Processor", "Cypress M8C microprocessor", "Renesas R32C series microprocessors", "NXP Semiconductors TriMedia architecture family", "QUALCOMM DSP6 Processor", "Intel 8051 and variants", "STMicroelectronics STxP7x family of configurable and extensible RISC processors", "Andes Technology compact code size embedded RISC processor family", "Cyan Technology eCOG1X family", "Dallas Semiconductor MAXQ30 Core Micro-controllers",
        "New Japan Radio (NJR) 16-bit DSP Processor", "M2000 Reconfigurable RISC Microprocessor", "Cray Inc. NV2 vector architecture", "Renesas RX family", "Imagination Technologies META processor architecture", "MCST Elbrus general purpose hardware architecture", "Cyan Technology eCOG16 family", "National Semiconductor CompactRISC CR16 16-bit microprocessor", "Freescale Extended Time Processing Unit", "Infineon Technologies SLE9X core",
        "Intel L10M", "Intel K10M", "Reserved", "ARM 64-bit architecture (AARCH64)", "Reserved", "Atmel Corporation 32-bit microprocessor family", "STMicroeletronics STM8 8-bit microcontroller", "Tilera TILE64 multicore architecture family", "Tilera TILEPro multicore architecture family", "Xilinx MicroBlaze 32-bit RISC soft processor core",
        "NVIDIA CUDA architecture", "Tilera TILE-Gx multicore architecture family", "CloudShield architecture family", "KIPO-KAIST Core-A 1st generation processor family", "KIPO-KAIST Core-A 2nd generation processor family", "Synopsys ARCompact V2", "Open8 8-bit RISC soft processor core", "Renesas RL78 family", "Broadcom VideoCore V processor", "Renesas 78KOR family",
        "Freescale 56800EX Digital Signal Controller (DSC)",
        "Beyond BA1 CPU architecture", "Beyond BA2 CPU architecture", "XMOS xCORE processor family", "Microchip 8-bit PIC(r) family", "Reserved", "Reserved", "Reserved", "Reserved", "Reserved",
        "KM211 KM32 32-bit processor", "KM211 KMX32 32-bit processor", "KM211 KMX16 16-bit processor", "KM211 KMX8 8-bit processor", "KM211 KVARC processor", "Paneve CDP architecture family", "Cognitive Smart Memory Processor", "iCelero CoolEngine", "Nanoradio Optimized RISC", "CSR Kalimba architecture family",
        "Zilog Z80"
    };

    static String abis[] = {
        "None", "Hewlett-Packard HP-UX", "NetBSD", "GNU", "Linux", "Sun Solaris", "AIX", "IRIX", "FreeBSD", "Compaq TRU64 UNIX", "Novell Modesto", "Open BSD", "Open VMS", "Hewlett-Packard Non-Stop Kernel", "Amiga Research OS", "The FenixOS highly scalable multi-core OS"
    };

    static String p_types[] = {
        "Null", "Loadable segment", "Dynamic linking", "Interpreter", "Note", "Shared Library", "Program Header"
    };

    static String sh_types[] = {
        "Null", "Program code", "Symbol table", "String table", "Relocation entries", "Symbol hash table", "Dynamic linking information", "Note", "Empty data", "Relocation entries", "Reserved", "Dynamic symbol table"
    };

    public class Elf {
        public int     e_type;
        public int     e_machine;
        public long    e_version;
        public long    e_entry;
        public long    e_phoff;
        public long    e_shoff;
        public long    e_flags;
        public int     e_ehsize;
        public int     e_phentsize;
        public int     e_phnum;
        public int     e_shentsize;
        public int     e_shnum;
        public int     e_shstrndx;

        public byte     ei_class;
        public byte     ei_data;
        public byte     ei_version;
        public byte     ei_osabi;
        public byte     ei_abiversion;

        public long     p_type;
        public long     p_offset;
        public long     p_vaddr;
        public long     p_paddr;
        public long     p_filesz;
        public long     p_memsz;
        public long     p_flags;
        public long     p_align;

        public long     shst_name;
        public long     shst_type;
        public long     shst_flags;
        public long     shst_addr;
        public long     shst_offset;
        public long     shst_size;
        public long     shst_link;
        public long     shst_info;
        public long     shst_addralign;
        public long     shst_entsize;

        RandomAccessFile file;

        public class Symbol implements Comparable {
            public String name;
            public long offset;

            public long     st_name;
            public long     st_value;
            public long     st_size;
            public byte     st_info;
            public byte     st_other;
            public int      st_shndx;

            public Symbol(long off) {
                try {
                    file.seek(off);
                    st_name = readInt();
                    st_value = readInt();
                    st_size = readInt();
                    st_info = readByte();
                    st_other = readByte();
                    st_shndx = readShort();
                    offset = off;
                    if (st_name > 0) {
                        name = loadString(".strtab", st_name);
                    } else {
                        name = "";
                    }
                } catch (Exception e) {
                    Base.error(e);
                }
            }
            public Section getSection() {
                for (Section s : sections.values()) {
                    if (st_value >= s.sh_addr && st_value < s.sh_addr + s.sh_size) {
                        return s;
                    }
                }
                return null;
            }
            public String toString() {
                Section s = getSection();
                if (s != null) {
                    return String.format("%20s 0x%08X 0x%08X %s", s.name, (int)st_value, (int)st_size, name);
                } else {
                    return String.format("%20s 0x%08X 0x%08X %s", "", (int)st_value, (int)st_size, name);
                }
            }

            public int compareTo(Object o) {
                if (!(o instanceof Symbol)) {
                    return 0;
                }
                Symbol s = (Symbol)o;
                Long me = st_size;
                Long other = s.st_size;
                if (me.equals(other)) {
                    me = s.st_value;
                    other = st_value;
                }
                return me.compareTo(other);
            }
        }

        public class Section implements Comparable {
            
            public long offset;
            public long     sh_name;
            public long     sh_type;
            public long     sh_flags;
            public long     sh_addr;
            public long     sh_offset;
            public long     sh_size;
            public long     sh_link;
            public long     sh_info;
            public long     sh_addralign;
            public long     sh_entsize;

            public String   name;
            
            public Section(long off) {
                try {
                    offset = off;
                    file.seek(offset);
                    sh_name = readInt();
                    sh_type = readInt();
                    sh_flags = readInt();
                    sh_addr = readInt();
                    sh_offset = readInt();
                    sh_size = readInt();
                    sh_link = readInt();
                    sh_info = readInt();
                    sh_addralign = readInt();
                    sh_entsize = readInt();
                    name = loadString(sh_name);
                } catch (Exception e) {
                    Base.error(e);
                }
            }

            public void seek() {
                try {
                    file.seek(offset);
                } catch (Exception e) {
                    Base.error(e);
                } 
            }

            public String toString() {
                return name;
            }

            public int compareTo(Object o) {
                if (!(o instanceof Section)) {
                    return 0;
                }
                Section s = (Section)o;
                return name.compareTo(s.name);
            }
        }

        public HashMap<String, Section> sections;
        public ArrayList<Symbol> symbols;

        public Elf(RandomAccessFile f) {
            file = f;
            try {
                file.seek(4);
                ei_class = readByte();   // File class
                ei_data = readByte();    // Data encoding
                ei_version = readByte(); // File version
                ei_osabi = readByte(); // ABI
                ei_abiversion = readByte(); // ABI Version

                file.seek(16);

                e_type = readShort();
                e_machine = readShort();
                e_version = readInt();
                e_entry = readInt();
                e_phoff = readInt();
                e_shoff = readInt();
                e_flags = readInt();
                e_ehsize = readShort();
                e_phentsize = readShort();
                e_phnum = readShort();
                e_shentsize = readShort();
                e_shnum = readShort();
                e_shstrndx = readShort();

                loadStringTableHeader();

                sections = new HashMap<String, Section>();
                
                for (int i = 0; i < e_shnum; i++) {
                    long offset = e_shoff + (i * e_shentsize);
                    Section s = new Section(offset);
                    sections.put(s.name, s);
                }

                Section symtab = sections.get(".symtab");

                symbols = new ArrayList<Symbol>();
                long recs = symtab.sh_size / symtab.sh_entsize;
                for (long l = 0; l < recs; l++) {
                    long pos = symtab.sh_offset + (l * symtab.sh_entsize);
                    Symbol sym = new Symbol(pos);
                    symbols.add(sym);
                }

            } catch (Exception e) {
                Base.error(e);
            }
        }

        public void loadProgramHeader(long offset) {
            try {
                file.seek(offset);
                p_type = readInt();
                p_offset = readInt();
                p_vaddr = readInt();
                p_paddr = readInt();
                p_filesz = readInt();
                p_memsz = readInt();
                p_flags = readInt();
                p_align = readInt();
            } catch (Exception e) {
                Base.error(e);
            }
        }

        public void loadStringTableHeader() {
            try {
                long offset = e_shoff + (e_shstrndx * e_shentsize);
                file.seek(offset);
                shst_name = readInt();
                shst_type = readInt();
                shst_flags = readInt();
                shst_addr = readInt();
                shst_offset = readInt();
                shst_size = readInt();
                shst_link = readInt();
                shst_info = readInt();
                shst_addralign = readInt();
                shst_entsize = readInt();
            } catch (Exception e) {
                Base.error(e);
            }
        }

        public Byte readByte() {
            try {
                return file.readByte();
            } catch (Exception e) {
                Base.error(e);
            }
            return 0;
        }

        public Integer readShort() {
            try {
                if (ei_data == 2) {
                    return (int)file.readShort();
                }
                byte b1 = readByte();
                byte b2 = readByte();
                int s = ((b2 << 8) | b1) & 0xFFFF;
                return s;
            } catch (Exception e) {
                Base.error(e);
            }
            return 0;
        }

        public Long readInt() {
            try {
                if (ei_data == 2) {
                    return (long)file.readInt();
                }
                byte b1 = readByte();
                byte b2 = readByte();
                byte b3 = readByte();
                byte b4 = readByte();
                long l1 = (b4 << 24) & 0xFF000000;
                long l2 = (b3 << 16) & 0x00FF0000;
                long l3 = (b2 << 8) & 0x0000FF00;
                long l4 = (b1) & 0x000000FF;
                long l = (l1 | l2 | l3 | l4) & 0xFFFFFFFF;
                return l;
            } catch (Exception e) {
                Base.error(e);
            }
            return (long)0;
        }

        public String loadString(String table, long index) {
            try {
                Section sec = sections.get(table);
                if (sec == null) {
                    return "";
                }
                long offset = sec.sh_offset + index;
                file.seek(offset);
                String out = "";
                byte b = readByte();
                char c = ((char)b);
                while (c != '\0') {
                    out += c;
                    b = readByte();
                    c = ((char)b);
                }
                return out;
            } catch (Exception e) {
                Base.error(e);
            }
            return "";
        }

        public String loadString(long index) {
            try {
                long offset = shst_offset + index;
                file.seek(offset);
                String out = "";
                byte b = readByte();
                char c = ((char)b);
                while (c != '\0') {
                    out += c;
                    b = readByte();
                    c = ((char)b);
                }
                return out;
            } catch (Exception e) {
                Base.error(e);
            }
            return "";
        }

        public String dumpRawData(long offset, long start, long size) {
            try {
                StringBuilder out = new StringBuilder();
                String text = "";

                file.seek(start);

                out.append(String.format("\t0x%08X: ", (int)(offset) & 0xFFFFFFFF));

                int ctr = 0;
                for (long i = 0; i < size; i++) {
                    byte b = readByte();
                    out.append(String.format("%02X ", (int)b & 0xFF));
                    if (b < 32 || b > 126) {
                        text += ".";
                    } else {
                        text += ((char)b);
                    }
                    ctr++;
                    if (ctr == 16) {
                        ctr = 0;
                        out.append(text);
                        text = "";
                        out.append(String.format("\n\t0x%08X: ", (int)(offset + file.getFilePointer() - start) & 0xFFFFFFFF));
                    }
                }           
                if (ctr != 0) {
                    while (ctr < 16) {
                        out.append("   ");
                        text = text + " ";
                        ctr++;
                    }
                    out.append(text);
                }
                out.append("\n");

                return out.toString();

            } catch (Exception e) {
                Base.error(e);
            }
            return "";
        }
    }

    public static HashMap<String, String> pluginInfo = null;
    public static void setInfo(HashMap<String, String>info) {
        pluginInfo = info;
    }
    public static String getInfo(String item) {
        return pluginInfo.get(item);
    }

    public ElfInspector(Editor e) {
        editor = e;
    }
    public ElfInspector(EditorBase e) {
        editorTab = e;
    }

    public void inspectFile(String filename) {
        try {
            elf = new RandomAccessFile(filename, "r");
            if (elf == null) {
                editor.error("Unable to open " + filename);
                return;
            }

            Elf elfFile = new Elf(elf);

            JFrame dialog = new JFrame();
            dialog.setLayout(new BorderLayout());

            RSyntaxTextArea text = new RSyntaxTextArea();

            StringBuilder sb = new StringBuilder();

            sb.append("Type:    ");
            if (elfFile.e_type < 5) {  
                sb.append(types[elfFile.e_type]);
            } else {
                sb.append(elfFile.e_type);
            }
            sb.append("\n");

            sb.append("Machine: ");
            if (elfFile.e_machine < 221) {
                sb.append(machines[elfFile.e_machine]);
            } else {
                sb.append("Unknown (");
                sb.append(elfFile.e_machine);
                sb.append(")");
            }
            sb.append("\n");

            sb.append("Endian:  ");
            if (elfFile.ei_data == 1) {
                sb.append("Little");
            } else if (elfFile.ei_data == 2) {
                sb.append("Big");
            } else {
                sb.append("Invalid");
            }
            sb.append("\n");

            sb.append("Version: ");
            sb.append(elfFile.e_version);
            sb.append("\n");

            if (elfFile.ei_osabi > 0) {
                sb.append("ABI:     ");
                if (elfFile.ei_osabi < 17) {
                    sb.append(abis[elfFile.ei_osabi]);
                } else {
                    sb.append("Unknown");
                }
                sb.append(" Version ");
                sb.append((int)elfFile.ei_abiversion);
                sb.append("\n");
            }

            sb.append("Entry:   ");
            sb.append(String.format("0x%08X", (int)elfFile.e_entry));
            sb.append("\n");

            sb.append("\n");

            sb.append("Symbols:\n\n");

            Elf.Section[] secs = elfFile.sections.values().toArray(new Elf.Section[elfFile.sections.size()]);
            Arrays.sort(secs);

            Elf.Symbol[] syms = elfFile.symbols.toArray(new Elf.Symbol[elfFile.symbols.size()]);
            Arrays.sort(syms, Collections.reverseOrder());
            for (Elf.Section sec : secs) {

                boolean hasEntries = false;
                for (Elf.Symbol s : syms) {
                    Elf.Section ssec = s.getSection();
                    if ((ssec != null) && (ssec.name.equals(sec.name))) {
                        sb.append(s);
                        sb.append("\n");
                        hasEntries = true;
                    }
                }
                if (hasEntries) {
                    sb.append("\n");
                }
            }

            for (Elf.Symbol s : syms) {
                Elf.Section ssec = s.getSection();
                if (ssec == null) {
                    sb.append(s);
                    sb.append("\n");
                }
            }
                

            text.setText(sb.toString());

            text.setCaretPosition(0);

            text.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);

            RTextScrollPane scroll = new RTextScrollPane(text);
            text.setCodeFoldingEnabled(true);
            scroll.setFoldIndicatorEnabled(true);

            FoldManager fm = text.getFoldManager();
            fm.clear();

            dialog.add(scroll, BorderLayout.CENTER);

            dialog.setSize(600, 500);
            dialog.setVisible(true);

        } catch (Exception e) {
            Base.error(e);
        }
    }

    public void populateContextMenu(JPopupMenu menu, int flags, DefaultMutableTreeNode node) {
        if (flags == (Plugin.MENU_TREE_FILE | Plugin.MENU_TOP)) {
            Object o = node.getUserObject();
            if (o instanceof File) {
                File f = (File)o;
                if (f.getName().endsWith(".elf")) {
                    JMenuItem item = new JMenuItem("Inspect ELF binary file");
                    item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                           inspectFile(e.getActionCommand());
                        }
                    });
                    item.setActionCommand(f.getAbsolutePath());
                    menu.add(item);
                } else if (f.getName().endsWith(".o")) {
                    JMenuItem item = new JMenuItem("Inspect ELF object file");
                    item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                           inspectFile(e.getActionCommand());
                        }
                    });
                    item.setActionCommand(f.getAbsolutePath());
                    menu.add(item);
                }
            }
        }
    }

    public void populateMenu(JMenu menu, int flags) {
    }

    public void addToolbarButtons(JToolBar toolbar, int flags) {
    }

    public ImageIcon getFileIconOverlay(File f) { return null; }

}
