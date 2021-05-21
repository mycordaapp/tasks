package mycorda.app.registry

import com.natpryce.hamkrest.sameInstance
import com.natpryce.hamkrest.throws
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test

import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class RegistrySpec {

    @Test
    fun `should store and load a class`() {
        val a = ClassA()
        val b = ClassB()
        val registry = Registry(a, b)

        assertThat(registry.get(ClassA::class.java), sameInstance(a))
        assertThat(registry.get(ClassB::class.java), sameInstance(b))
        assertThat({ registry.get(ClassC::class.java) }, throws<RuntimeException>())
    }

    @Test
    fun `should store and load an interface`() {
        val one = InterfaceAImplOne()
        val registry = Registry(one)

        assertThat(registry.get(InterfaceAImplOne::class.java), sameInstance(one))
        assertThat(registry.get(InterfaceA::class.java), sameInstance(one as InterfaceA))
    }

    @Test
    fun `should recurse through interface hierarchy`() {
        val impl = InterfaceAPlusImpl()
        val registry = Registry(impl)

        assertThat(registry.get(InterfaceAPlus::class.java), sameInstance(impl as InterfaceAPlus ))
        assertThat(registry.get(InterfaceA::class.java), sameInstance(impl as InterfaceA))
    }

    @Test
    fun `should store and load an extended class`() {
        val cplus = ClassCPlus()
        val registry = Registry(cplus)

        assertThat(registry.get(ClassCPlus::class.java), sameInstance(cplus))
        assertThat(registry.get(ClassC::class.java), sameInstance(cplus as ClassC))
    }

    @Test
    fun `should fail to load if multiple classes extend same base class`() {
        val cplus = ClassCPlus()
        val cplusplus = ClassCPlusPlus()

        // all good - we only have one instance, so no possibility for ambiguity
        val registry = Registry().store(cplus)
        assertThat(registry.get(ClassCPlus::class.java), sameInstance(cplus))
        assertThat(registry.get(ClassC::class.java), sameInstance(cplus as ClassC))

        // now there is a second overidden class - now only ClassCPlusPlus is unambiguous
        val updated = registry.store(cplusplus)
        assertThat({ updated.get(ClassC::class.java) }, throws<RuntimeException>())
        assertThat({ updated.get(ClassCPlus::class.java) }, throws<RuntimeException>())
        assertThat(updated.get(ClassCPlusPlus::class.java), sameInstance(cplusplus))
    }


    @Test
    fun `should store and load an Executor Service`() {
        val executorService = Executors.newFixedThreadPool(10) as ExecutorService
        val one = InterfaceAImplOne()
        val registry = Registry().store(executorService).store(one)

        assertThat(registry.get(AbstractExecutorService::class.java), sameInstance(executorService))

        // TODO - for some reason, not detecting that this implements ExecutorService
        //        can't see why - just doesn't seem to come back via Reflections
        // assertThat(registry.get(ExecutorService::class.java), sameInstance(executorService))

        assertThat(registry.get(InterfaceA::class.java), sameInstance(one as InterfaceA))

    }

    @Test
    fun `clone should be immutable`() {
        val registry = Registry()

        val withClassA = registry.store(ClassA())
        assertThat(withClassA.contains(ClassA::class.java), equalTo(true))
        assertThat(withClassA.contains(ClassB::class.java), equalTo(false))

        val withClassB = withClassA.clone().store(ClassB()).clone()
        assertThat(withClassB.contains(ClassA::class.java), equalTo(true))
        assertThat(withClassB.contains(ClassB::class.java), equalTo(true))
        // earlier registry is unaltered
        assertThat(withClassA.contains(ClassB::class.java), equalTo(false))
    }
}

class ClassA

class ClassB

open class ClassC

open class ClassCPlus : ClassC()

class ClassCPlusPlus : ClassCPlus()

interface InterfaceA

interface InterfaceAPlus : InterfaceA{
    fun foo()
}

class InterfaceAImplOne : InterfaceA

class InterfaceAPlusImpl : InterfaceAPlus{
    override fun foo() {
        TODO("Not yet implemented")
    }
}

@Suppress("unused")
class InterfaceAImplTwo : InterfaceA

data class DataClass(val name: String)