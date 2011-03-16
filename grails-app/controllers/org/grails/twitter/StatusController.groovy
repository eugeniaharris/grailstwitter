package org.grails.twitter

import grails.plugins.springsecurity.Secured

@Secured('IS_AUTHENTICATED_FULLY')
class StatusController {

    def statusService

    def index = {
        def messages = statusService.currentUserTimeline()
        [statusMessages: messages]
    }

    def updateStatus = {
        statusService.updateStatus params.message
        def messages = statusService.currentUserTimeline()
        render template: 'statusMessages', collection: messages, var: 'statusMessage'
    }

    def follow = {
        statusService.follow params.long('id')
        redirect action: 'index'
    }
}
