package mycorda.app.tasks

/**
 * For places where generics is demanding in instance, but we don't need anything
 */
class NotRequired {
    companion object {
        fun instance(): NotRequired {
            return NotRequired()
        }
    }
}