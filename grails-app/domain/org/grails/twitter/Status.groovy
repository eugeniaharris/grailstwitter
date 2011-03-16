package org.grails.twitter

import org.grails.twitter.auth.Person

class Status {

    def grailsApplication

    String message
    Person author
    Date dateCreated

    static constraints = {
        message blank:false
    }

    transient jmsService
    transient afterInsert = {
        // Note '<app name>.<service name of listener>' (e.g., grailstwitter/status)
        jmsService.send "${grailsApplication.metadata.getApplicationName()}.status", author.username
    }
}
