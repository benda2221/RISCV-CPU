import chisel3._
import chisel3.util._

class CPUIO extends Bundle {
    val iAddr   = Output(UInt(32.W))
    val iData   = Input(UInt(32.W))
    val dAddr   = Output(UInt(32.W))
    val dWdata  = Output(UInt(32.W))
    val dWstrb  = Output(UInt(4.W))
    val dRdata  = Input(UInt(32.W))
}

class CPU extends Module {
    val io = IO(new CPUIO())

    val pc = Module(new PC())
    val decoder = Module(new Decoder())
    val regfile = Module(new RegisterFile())
    val alu = Module(new ALU())
    val branch = Module(new Branch())
    val bypass = Module(new Bypass())
    val hazard = Module(new Hazard())

    /* fetch stage */
    pc.io.jumpEn    := branch.io.jumpEn
    pc.io.jumpTgt   := branch.io.jumpTgt
    pc.io.stall     := hazard.io.hazardEn
    io.iAddr        := pc.io.pc

    val instPkgIF   = WireDefault(0.U.asTypeOf(new InstructionPackage()))
    instPkgIF.inst  := io.iData
    instPkgIF.pc    := pc.io.pc
    instPkgIF.vld   := true.B

    /* decode stage */
    val instPkgIDIn = ShiftRegister(
        Mux(branch.io.jumpEn, 0.U.asTypeOf(new InstructionPackage()), instPkgIF), 1, 0.U.asTypeOf(new InstructionPackage()), !hazard.io.hazardEn || branch.io.jumpEn
    )

    decoder.io.inst := instPkgIF.inst
    val instPkgIDOut = WireDefault(decoder.io.instPkg)
    instPkgIDOut.pc   := instPkgIDIn.pc
    instPkgIDOut.inst := instPkgIDIn.inst
    instPkgIDOut.vld  := instPkgIDIn.vld

    regfile.io.rs1    := instPkgIDOut.rs1
    regfile.io.rs2    := instPkgIDOut.rs2
    regfile.io.rd     := instPkgIDOut.rd

    instPkgIDOut.rs1Data := regfile.io.rs1Data
    instPkgIDOut.rs2Data := regfile.io.rs2Data

    /* execute stage */
    val instPkgEXIn = ShiftRegister(
        Mux(hazard.io.hazardEn || branch.io.jumpEn, 0.U.asTypeOf(new InstructionPackage()), instPkgIDOut), 1, 0.U.asTypeOf(new InstructionPackage()), !hazard.io.hazardEn || branch.io.jumpEn
    )
    alu.io.src1 := Mux1H(instPkgEXIn.aluSrc1, VecInit(instPkgEXIn.pc, Mux(bypass.io.src1BypassEn, bypass.io.src1BypassData, instPkgEXIn.rs1Data), 0.U))
    alu.io.src2 := Mux1H(instPkgEXIn.aluSrc2, VecInit(instPkgEXIn.imm, Mux(bypass.io.src2BypassEn, bypass.io.src2BypassData, instPkgEXIn.rs2Data), 4.U))
    alu.io.op    := instPkgEXIn.op(3, 0)

    branch.io.src1 := Mux(bypass.io.src1BypassEn, bypass.io.src1BypassData, instPkgEXIn.rs1Data)
    branch.io.src2 := Mux(bypass.io.src2BypassEn, bypass.io.src2BypassData, instPkgEXIn.rs2Data)
    branch.io.op    := instPkgEXIn.op(4, 0)
    branch.io.pc    := instPkgEXIn.pc
    branch.io.imm   := instPkgEXIn.imm

    val instPkgEXOut = WireDefault(instPkgEXIn)
    instPkgEXOut.aluResult := alu.io.result
    instPkgEXOut.rs2Data   := Mux(bypass.io.src2BypassEn, bypass.io.src2BypassData, instPkgEXIn.rs2Data)

    /* memory stage */
    val instPkgLSIn = ShiftRegister(
        instPkgEXOut, 1, 0.U.asTypeOf(new InstructionPackage()), true.B
    )

    io.dAddr := instPkgLSIn.aluResult
    io.dWdata := instPkgLSIn.rs2Data
    io.dWstrb := MuxLookup(instPkgLSIn.op(1, 0), 0.U(4.W))(Seq(
        0.U -> 0x1.U(4.W),
        1.U -> 0x3.U(4.W),
        2.U -> 0xf.U(4.W)
    )) & Fill(4, instPkgLSIn.op(6))

    instPkgLSIn.memResult := MuxLookup(instPkgLSIn.op(2, 0), 0.U(32.W))(Seq(
        0.U -> Fill(24, io.dRdata(7)) ## io.dRdata(7, 0),
        1.U -> Fill(16, io.dRdata(15)) ## io.dRdata(15, 0),
        2.U -> io.dRdata,
        4.U -> 0.U(24.W) ## io.dRdata(7, 0),
        5.U -> 0.U(16.W) ## io.dRdata(15, 0),
    ))

    val instPkgLSOut = WireDefault(instPkgLSIn)
    instPkgLSOut.memResult := instPkgLSIn.memResult

    /* write back stage */
    val instPkgWBIn = ShiftRegister(
        instPkgLSOut, 1, 0.U.asTypeOf(new InstructionPackage()), true.B
    )
    regfile.io.rd     := instPkgWBIn.rd
    regfile.io.rdData := Mux(instPkgWBIn.op(5), instPkgWBIn.memResult, instPkgWBIn.aluResult)
    regfile.io.rdVld  := instPkgWBIn.rdVld
    
    val instPkgWBOut = WireDefault(instPkgWBIn)
    instPkgWBOut.aluResult := Mux(instPkgWBIn.op(5), instPkgWBIn.memResult, instPkgWBIn.aluResult)


    // bypass
    bypass.io.instPkgEX := instPkgEXIn
    bypass.io.instPkgLS := instPkgLSIn
    bypass.io.instPkgWB := instPkgWBOut

    // hazard
    hazard.io.instPkgID := instPkgIDOut
    hazard.io.instPkgEX := instPkgEXOut

    
}