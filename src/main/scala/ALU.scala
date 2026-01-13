import chisel3._
import chisel3.util._


object ALUOpcode {
    val ADD     = 0x0.U(4.W)
    val SLL     = 0x1.U(4.W)
    val SLT     = 0x2.U(4.W)
    val SLTU    = 0x3.U(4.W)
    val XOR     = 0x4.U(4.W)
    val SRL     = 0x5.U(4.W)
    val OR      = 0x6.U(4.W)
    val AND     = 0x7.U(4.W)
    val SUB     = 0x8.U(4.W)
    val SRA     = 0xd.U(4.W)
}


class ALUIO extends Bundle {
    val src1    = Input(UInt(32.W))
    val src2    = Input(UInt(32.W))
    val op      = Input(UInt(4.W))
    val result  = Output(UInt(32.W))
}

class ALU extends Module {
    val io = IO(new ALUIO())

    val result = WireDefault(0.U(32.W))
    import ALUOpcode._

    switch(io.op){
        is(ADD){
            result := io.src1 + io.src2
        }
        is(SLL){
            result := io.src1 << io.src2(4, 0)
        }
        is(SLT){
            result := (io.src1.asSInt < io.src2.asSInt).asUInt
        }
        is(SLTU){
            result := io.src1.asUInt < io.src2.asUInt
        }
        is(XOR){
            result := io.src1 ^ io.src2
        }
        is(SRL){
            result := io.src1 >> io.src2(4, 0)
        }
        is(OR){
            result := io.src1 | io.src2
        }
        is(AND){
            result := io.src1 & io.src2
        }
        is(SUB){
            result := io.src1 - io.src2
        }
        is(SRA){
            result := (io.src1.asSInt >> io.src2(4, 0)).asUInt
        }
    }
    io.result := result
}