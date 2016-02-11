package com.patrickmcgeever.scala.akka.akkapersistence

import akka.actor.{Actor, ActorSystem, Props}
import akka.pattern.BackoffSupervisor
import akka.persistence.fsm.PersistentFSM.Shutdown
import com.patrickmcgeever.scala.akka.akkapersistence.Messages.{ContactCreated, CreateContact, RetrieveContact}

import scala.concurrent.duration.DurationInt
import scala.io.StdIn

object ContactApp extends App {

  println("Usage:")
  println("n = new contact")
  println("e = add email to existing contact")
  println("r = retrieve contact")
  println("exit = exit app")
  println

  val system = ActorSystem("ContactApp")
  val receiverActor = system.actorOf(Props[Receiver], "receiver")

  val contactChildProps = ContactActor.props(receiverActor);
  val contactSupervisorProps = BackoffSupervisor.props(
    contactChildProps,
    "contactActor",
    3.seconds,
    30.seconds,
    0.2)
  val contactSupervisorActor = system.actorOf(contactSupervisorProps, name = "contactSupervisor")

  var in = ""
  do {
    println("Select operation:")
    print("> ")
    in = StdIn.readLine()
    in match {
      case "n" => newContact()
      //      case "e" => addEmail()
      case "r" => retrieveContact()
      case "exit" => println("Exiting")
      case _ => println("Invalid selection")
    }
  } while (in != "exit")

  contactSupervisorActor ! Shutdown
  system.terminate()

  def newContact() = {
    println("Enter contact name:")
    print("> ")
    val name = StdIn.readLine()

    println("Enter contact telephone number:")
    print("> ")
    val telNo = StdIn.readLine()

    println("Enter contact email:")
    print("> ")
    val email = StdIn.readLine()

    val contact = Contact(name, Option(telNo), Option(email))

    contactSupervisorActor ! CreateContact(contact)
  }

  def retrieveContact() = {
    println("Enter contact name:")
    print("> ")
    val name = StdIn.readLine()

    contactSupervisorActor ! RetrieveContact(name)
  }

  class Receiver extends Actor {
    override def receive: Receive = {
      case ContactCreated => println("Contact created")
      case contact: Option[Contact] => {
        if(contact.nonEmpty) {
          val name = contact.get.name
          val telNo = contact.get.telNo.getOrElse("")
          val email = contact.get.email.getOrElse("")
          println(s"Name: $name")
          println(s"Telephone: $telNo")
          println(s"Email: $email")
        } else {
          println("Contact does not exist")
        }
      }
    }
  }
}
