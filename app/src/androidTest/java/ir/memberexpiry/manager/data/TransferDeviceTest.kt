package ir.memberexpiry.manager.data
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import ir.memberexpiry.manager.core.date.JalaliDate
import ir.memberexpiry.manager.core.transfer.*
import ir.memberexpiry.manager.data.local.MemberDatabase
import ir.memberexpiry.manager.data.repository.MemberInput
import ir.memberexpiry.manager.data.repository.MemberRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
class TransferDeviceTest {
    @Test fun importBackupRestoreAndRollback() = runBlocking {
        val db=Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,MemberDatabase::class.java).build()
        try {
            val repo=MemberRepository(db)
            val input=MemberInput("0000000001","علی","رضایی","رضا",JalaliDate(1405,9,3))
            repo.addMember(input)
            val original=repo.backup()
            val out=ByteArrayOutputStream(); BackupCodec.write(out,original)
            assertEquals(original,BackupCodec.read(out.toByteArray().inputStream()))
            assertEquals(0 to 0,repo.restore(original,false))
            repo.importMembers(listOf(ImportRow(input.copy(lastName="محمدی"))),ImportMode.MERGE)
            assertEquals("محمدی",repo.backup().members.single().lastName)
            assertTrue(runCatching { repo.restore(original,false) }.isFailure)
            assertEquals("محمدی",repo.backup().members.single().lastName)
            repo.importMembers(listOf(ImportRow(input)),ImportMode.APPEND)
            assertEquals(2,repo.backup().members.size)
            assertTrue(runCatching { repo.importMembers(listOf(ImportRow(input)),ImportMode.MERGE) }.isFailure)
            assertEquals(2,repo.backup().members.size)
            repo.importMembers(listOf(ImportRow(input)),ImportMode.REPLACE)
            assertEquals(1,repo.backup().members.size); assertEquals(1,repo.backup().logs.size)
            repo.restore(original,true)
            assertEquals(original,repo.backup())
        } finally { db.close() }
    }
}
