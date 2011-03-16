import geb.Page
import org.grails.twitter.Status
import org.grails.twitter.auth.Person

class StatusSpec extends grails.plugin.geb.GebSpec {

    // Notes:
    // - these tests can't be run from IDE because they access domain class, see:
    //   http://adhockery.blogspot.com/2011/02/running-geb-tests-from-your-ide.html
    // - use -functional option with grails test-app to avoid null pointer
    //   exception (presumably due to absence of unit/integration tests)
    // - final page output for each test case under target/test-reports/geb/<test>
    // - helpful posts on spock/geb testing collected at:
    //   http://docs.codehaus.org/display/GEB/Articles

    static final int MAX_STATUS = StatusPage.MAX_STATUS
    static final int PERSON_COUNT = Person.count()

    private Person burt = Person.findByUsername("burt")
    private Person peter = Person.findByUsername("peter")

    def "users in database"() {
        expect:
        burt
        peter
    }

    def "require login"() {
        when:
        to StatusPage

        then:
        at LoginPage
    }

    def "can log in"() {
        when:
        login burt

        then:
        at StatusPage
        isLoggedIn burt.username
    }

    def "can log out"() {
        when:
        login burt
        logout()

        then:
        at LoginPage
    }

    def "user sees own status update"() {
        when:
        login burt
        saySomething burt, "Hello world"

        then:
        true // saySomething validates
    }

    void "no more than max statuses displayed"() {
        when:
        login burt
        (MAX_STATUS + 1).times {
            saySomething burt, "something $it"
        }

        then:
        true // saySomething validates
    }

    void "search for non-existent user"() {
        when:
        login burt
        search "xyzzy"

        then:
        at SearchPage
        searchResults.size() == 0
    }

    void "search for exact user, case-insensitive"() {
        when:
        login burt
        search "LEDBROOK"

        then:
        at SearchPage
        searchResults.size() == 1
    }

    void "search for all users"() {
        when:
        login burt
        search "*"

        then:
        at SearchPage
        searchResults.size() == PERSON_COUNT
    }

    void "follow someone"() {
        when:
        login burt
        follow burt, peter

        then:
        at StatusPage
        assert burt.followed.contains(peter)
    }

    void "follower sees followee status"() {
        given:
        def msg = 'whatever'

        when:
        login burt
        saySomething burt, msg
        logout()

        login peter
        follow peter, burt

        then:
        at StatusPage
        verifyLatestStatus burt, msg
    }

    void "follower cache cleared"() {
        given:
        def msg = 'something else'

        when:
        login burt
        follow burt, peter
        logout()

        login peter
        saySomething peter, msg
        logout()

        login burt

        then:
        verifyLatestStatus peter, msg
    }


    private void login(Person person, String password = "password") {
        to LoginPage
        doLogin person.username, password
    }

    private void logout() {
        to StatusPage
        doLogout()
    }

    private boolean saySomething(Person who, String what) {
        if (!who || !what) {
            throw new IllegalStateException("Someone ($who) has to say something ($what) for the test")
        }
        to StatusPage
        int count = statusMessages.size()
        doUpdate what
        assert statusMessages.size() == (count == MAX_STATUS ? MAX_STATUS : count + 1)
        verifyLatestStatus who, what
    }

    private boolean verifyLatestStatus(Person who, String what) {
        String lookingFor = "$who.realName said $what"
        println "LATEST STATUS: $latestStatus (Looking for '$lookingFor')"
        latestStatus.contains(lookingFor)
    }

    private void search(String what) {
        to StatusPage
        doSearch what
    }

    private void follow(Person follower, Person followee) {
        search followee.realName
        at SearchPage
        doFollowFirst()
    }
}


class LoginPage extends Page {

    static url = "login/auth"
    static at = {
        println "TITLE: $title, URL: $pageUrl"
        title == "Login"
    }

    static content = {
        usernameField { $("input", name:"j_username") }
        passwordField { $("input", name:"j_password") }
        loginButton(to: StatusPage) { $("input", type:"submit") }
    }

    void doLogin(String username, String password) {
        usernameField.value username
        passwordField.value password
        loginButton.click()
    }
}

class StatusPage extends Page {

    static final int MAX_STATUS = 10

    static url = "status"
    static at = {
        println "TITLE: $title, URL: $pageUrl"
        title == "What Are You Doing?"
    }

    static content = {
        welcomeMsg { $("#welcome").text() }
        logoutLink(to:LoginPage) { $("a", name:"logout") }
        searchField { $("input", name:"q") }
        searchButton(to:SearchPage) { $("input", name:"Search") }
        statusUpdateField { $("textarea", name:"message") }
        statusUpdateButton(to:StatusPage) { $("input", name:"Update Status") }
        statusMessages(required: false) { $("div.statusMessage")*.text() }
        latestStatus { statusMessages[0] }
    }

    void doLogout() {
        logoutLink.click()
    }

    void doUpdate(String status) {
        if (!status) {
            throw new IllegalStateException("Empty status updates not supported")
        }
        statusUpdateField.value status
        statusUpdateButton.click()

        // XXX: Thought these two should be equivalent but below didn't work
        //waitFor { ! statusUpdateField.value() }
        waitFor { ! $("textarea", name:"message").value() }
    }

    void doSearch(String term) {
        searchField.value term
        searchButton.click()
    }

    boolean isLoggedIn(String username) {
        welcomeMsg == "Welcome $username"
    }
}

class SearchPage extends Page {

    static url = "searchable"
    static at = {
        println "TITLE: $title, URL: $pageUrl"
        title == "Grails"
    }

    static content = {
        searchResults(required:false) { $("div.name")*.text() }
        firstFollowLink(required:false, to:StatusPage) { $("a.followLink", 0) }
    }

    void doFollowFirst() {
        if (!firstFollowLink) {
            throw new IllegalStateException("No results, can't follow")
        }
        firstFollowLink.click()
    }
}
