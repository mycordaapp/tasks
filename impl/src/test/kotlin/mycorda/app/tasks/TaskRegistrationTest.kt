package mycorda.app.tasks

import com.natpryce.hamkrest.*
import com.natpryce.hamkrest.assertion.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TaskRegistrationTest
{

    @Test
    fun `should create by class name`() {
        val registrations = TaskRegistrations.fromClazzName("mycorda.app.tasks.test.TaskRegistrationsExample")
        assertThat(registrations::class.simpleName, equalTo("TaskRegistrationsExample"))

        val matcher : Matcher<TaskRegistrations?> = present()
        assertMatch(matcher(registrations))
    }

    @Test
    fun `should throw exception if class doesnt exist` () {
        assertThat(
            { TaskRegistrations.fromClazzName("com.example.Missing") },
            throws<TaskException>(
                has(
                    Exception::message,
                    present(equalTo("Problem instantiating `com.example.Missing`. ClassNotFoundException"))
                )
            )
        )
    }

    @Test
    fun `should throw exception if class missing default constructor` () {
        assertThat(
            { TaskRegistrations.fromClazzName("mycorda.app.tasks.test.TaskRegistrationsWithBadConstructor") },
            throws<TaskException>(
                has(
                    Exception::message,
                    present(equalTo("Problem instantiating `mycorda.app.tasks.test.TaskRegistrationsWithBadConstructor`. Couldn't find a no args constructor"))
                )
            )
        )
    }



    private fun assertMatch(result: MatchResult) {
        assertEquals(MatchResult.Match, result)
    }
}