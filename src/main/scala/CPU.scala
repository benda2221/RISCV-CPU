import chisel3._
import chisel3.util._

class CPUIO extends Bundle {
    val iAddr   = Output(UInt(32.W))
    val iData   = Input(UInt(32.W))
    val dAddr   = Output(UInt(32.W))
    val dWdata  = Output(UInt(32.W))
    val dWstrb  = Output(UInt(4.W))
    val dRdata  = Input(UInt(32.W))
    
    // Debug interface for emulator
    val dbg_wb_valid = Output(Bool())      // Write-back stage valid signal
    val dbg_wb_pc    = Output(UInt(32.W))  // Write-back stage PC
    val dbg_rf       = Output(Vec(32, UInt(32.W)))  // Register file for debug access
}

class CPU extends Module {
    val io = IO(new CPUIO())

    val pc = Module(new PC())
    val decoder = Module(new Decoder())
    val regfile = Module(new RegisterFile())
    val alu = Module(new ALU())
    val branch = Module(new Branch())
    val srt2 = Module(new SRT2())
    val multiply = Module(new MulBooth2Wallce())
    val bypass = Module(new Bypass())
    val hazard = Module(new Hazard())

    // SRT2 busy stall signal (defined at module level)
    val srt2BusyStall = WireDefault(false.B)

    /* fetch stage */
    pc.io.jumpEn    := branch.io.realJp
    pc.io.jumpTgt   := branch.io.jumpTgt
    pc.io.stall     := hazard.io.hazardEn || srt2BusyStall
    io.iAddr        := pc.io.pc

    val instPkgIF   = WireDefault(0.U.asTypeOf(new InstructionPackage()))
    instPkgIF.inst  := io.iData
    instPkgIF.pc    := pc.io.pc
    instPkgIF.vld   := true.B

    /* decode stage */
    val instPkgIDIn = ShiftRegister(
        Mux(branch.io.realJp, 0.U.asTypeOf(new InstructionPackage()), instPkgIF), 1, 0.U.asTypeOf(new InstructionPackage()), !hazard.io.hazardEn || branch.io.realJp
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
        Mux(hazard.io.hazardEn || branch.io.realJp, 0.U.asTypeOf(new InstructionPackage()), instPkgIDOut), 1, 0.U.asTypeOf(new InstructionPackage()), !hazard.io.hazardEn || branch.io.realJp
    )
    
    // ALU connections
    val aluSrc1 = Mux1H(instPkgEXIn.aluSrc1, VecInit(instPkgEXIn.pc, Mux(bypass.io.src1BypassEn, bypass.io.src1BypassData, instPkgEXIn.rs1Data), 0.U))
    val aluSrc2 = Mux1H(instPkgEXIn.aluSrc2, VecInit(instPkgEXIn.imm, Mux(bypass.io.src2BypassEn, bypass.io.src2BypassData, instPkgEXIn.rs2Data), 4.U))
    alu.io.src1 := aluSrc1
    alu.io.src2 := aluSrc2
    alu.io.op   := instPkgEXIn.op(4, 0)  // Changed from 3,0 to 4,0 for 5-bit op

    // Branch connections
    branch.io.src1 := Mux(bypass.io.src1BypassEn, bypass.io.src1BypassData, instPkgEXIn.rs1Data)
    branch.io.src2 := Mux(bypass.io.src2BypassEn, bypass.io.src2BypassData, instPkgEXIn.rs2Data)
    branch.io.op    := instPkgEXIn.op(4, 0)
    branch.io.pc    := instPkgEXIn.pc
    branch.io.imm   := instPkgEXIn.imm
    branch.io.predOffset := instPkgEXIn.pc + 4.U  // Simple prediction: next instruction

    // Determine operation type
    // From Decoder: 
    // - Standard ALU: op = funct7[5] ## funct3 (5-bit, op(4) may be 0 or 1)
    // - Multiply: op = funct3 (4-bit, op(3,0) = 0-3, op(4) = 0, op(2) = 0)
    // - Division: op = funct3 (4-bit, op(3,0) = 4-7, op(4) = 0, op(2) = 1)
    // We can distinguish by checking op(4) and op(3,0) range
    // Multiplication: op(4) = 0, op(3,0) < 4, op(2) = 0
    // Division: op(4) = 0, op(3,0) >= 4, op(2) = 1
    // Standard ALU: op(4) may be 1, or op(3,0) matches standard ALU patterns
    val isMulOp = !instPkgEXIn.op(4) && !instPkgEXIn.op(2) && instPkgEXIn.op(3, 0) < 4.U
    val isDivOp = !instPkgEXIn.op(4) && instPkgEXIn.op(2) && instPkgEXIn.op(3, 0) >= 4.U
    
    // SRT2 connections (for division/remainder operations)
    srt2.io.src1 := aluSrc1
    srt2.io.src2 := aluSrc2
    srt2.io.op   := instPkgEXIn.op(3, 0)  // 4-bit op for SRT2
    
    // Multiply connections (for multiplication operations)
    multiply.io.src1 := aluSrc1
    multiply.io.src2 := aluSrc2
    multiply.io.op   := instPkgEXIn.op(3, 0)  // 4-bit op for Multiply
    multiply.io.divBusy := srt2.io.busy  // Stall multiply when division is busy
    
    // Stall pipeline when SRT2 is busy with a div/rem operation
    srt2BusyStall := isDivOp && srt2.io.busy

    val instPkgEXOut = WireDefault(instPkgEXIn)
    // Select result from ALU, Multiply, or SRT2 based on operation type
    instPkgEXOut.aluResult := Mux1H(Seq(
        (isMulOp, multiply.io.res),
        (isDivOp && srt2.io.ready, srt2.io.res),
        (true.B, alu.io.res)  // Default to ALU result
    ))
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

    // Debug interface connections
    io.dbg_wb_valid := instPkgWBIn.vld  // Write-back stage valid signal
    io.dbg_wb_pc    := instPkgWBIn.pc   // Write-back stage PC
    io.dbg_rf       := regfile.io.dbg_rf  // Register file for debug access

    // bypass
    bypass.io.instPkgEX := instPkgEXIn
    bypass.io.instPkgLS := instPkgLSIn
    bypass.io.instPkgWB := instPkgWBOut

    // hazard
    hazard.io.instPkgID := instPkgIDOut
    hazard.io.instPkgEX := instPkgEXOut

    
}