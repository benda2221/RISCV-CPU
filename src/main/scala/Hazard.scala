import chisel3._
import chisel3.util._

class HazardIO extends Bundle {
    val instPkgID = Input(new InstructionPackage())
    val instPkgEX = Input(new InstructionPackage())
    val hazardEn  = Output(Bool())
}

class Hazard extends Module {
    val io = IO(new HazardIO())

    io.hazardEn := (io.instPkgID.rs1 === io.instPkgEX.rd || io.instPkgID.rs2 === io.instPkgEX.rd) && io.instPkgEX.op(5)
}