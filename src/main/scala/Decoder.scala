import chisel3._
import chisel3.util._

class DecoderIO extends Bundle {
    val inst       = Input(UInt(32.W))
    val instPkg    = Output(new InstructionPackage())

}

class Decoder extends Module {
    val io = IO(new DecoderIO())

    val instPkg = WireDefault(0.U.asTypeOf(new InstructionPackage()))

    val inst = io.inst
    val funct3 = inst(14, 12)
    val funct7 = inst(31, 25)

    instPkg.rd := inst(11, 7)
    instPkg.rdVld := inst(11, 7) =/= 0.U
    instPkg.rs1 := inst(19, 15)
    instPkg.rs2 := inst(24, 20)

    switch(inst(6, 0)){
        // lui
        is(0x37.U){
            instPkg.op := 0.U // ADD
            instPkg.aluSrc1 := 4.U // choose 0
            instPkg.aluSrc2 := 1.U // choose imm
            instPkg.imm := io.inst(31, 12) ## 0.U(12.W)
        }
        // auipc
        is(0x17.U){
            instPkg.op := 0.U // ADD
            instPkg.aluSrc1 := 1.U // choose pc
            instPkg.aluSrc2 := 1.U // choose imm
            instPkg.imm := io.inst(31, 12)
        }
        // jal
        is(0x6f.U){
            instPkg.op := 0.U // ADD
            instPkg.aluSrc1 := 1.U // choose pc
            instPkg.aluSrc2 := 4.U // choose 4
            instPkg.imm := Fill(12, inst(31)) ## inst(19, 12) ## inst(20) ## inst(30, 21) ## 0.U(1.W)
        }
        // jalr 
        is(0x67.U){
            instPkg.op := 0.U // ADD
            instPkg.aluSrc1 := 1.U // choose pc
            instPkg.aluSrc2 := 4.U // choose 4
            instPkg.imm := Fill(20, inst(31)) ## inst(31, 20)
        }
        // branch
        is(0x63.U){
            instPkg.op := 2.U(2.W) ## funct3
            instPkg.aluSrc1 := 1.U // choose pc
            instPkg.aluSrc2 := 1.U // choose imm
            instPkg.imm := Fill(20, inst(31)) ## inst(7) ## inst(30, 25) ## inst(11, 8) ## 0.U(1.W)
            instPkg.rdVld := false.B
        }
        // load
        is(0x03.U){
            instPkg.op := 4.U(3.W) ## funct3
            instPkg.aluSrc1 := 2.U // choose rs1
            instPkg.aluSrc2 := 1.U // choose imm
            instPkg.imm := Fill(20, inst(31)) ## inst(31, 20)
        }
        // store
        is(0x23.U){
            instPkg.op := 8.U(4.W) ## funct3
            instPkg.aluSrc1 := 2.U // choose rs1
            instPkg.aluSrc2 := 1.U // choose imm
            instPkg.imm := Fill(20, inst(31)) ## inst(31, 25) ## inst(11, 7)
            instPkg.rdVld := false.B
        }
        // arithmetic imm
        is(0x13.U){
            instPkg.op := Mux(funct3 === 0x5.U, funct7(5), 0.U) ## funct3
            instPkg.aluSrc1 := 2.U // choose rs1
            instPkg.aluSrc2 := 1.U // choose imm
            instPkg.imm := Fill(20, inst(31)) ## inst(31, 20)
        }
        // arithmetic reg
        is(0x33.U){
            instPkg.op := Mux(funct3 === 0x5.U, funct7(5), 0.U) ## funct3
            instPkg.aluSrc1 := 2.U // choose rs1
            instPkg.aluSrc2 := 2.U // choose rs2
        }
    }
    io.instPkg := instPkg
}