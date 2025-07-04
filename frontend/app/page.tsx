import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import Link from "next/link"

export default function HomePage() {
  return (
    <div className="container mx-auto px-4 py-16">
      <div className="text-center mb-12">
        <h1 className="text-4xl font-bold text-violet-500 mb-4">Smart Appointment Booking System</h1>
        <p className="text-xl text-white max-w-2xl mx-auto">
          Streamline your appointment scheduling with our modern, intuitive booking platform
        </p>
      </div>

      <div className="grid md:grid-cols-3 gap-8 max-w-4xl mx-auto">
        <Card>
          <CardHeader>
            <CardTitle>For Users</CardTitle>
            <CardDescription>Book appointments with ease</CardDescription>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-gray-600 mb-4">
              Browse available slots and book appointments with your preferred providers
            </p>
            <Link href="/signin">
              <Button className="w-full">Get Started</Button>
            </Link>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>For Providers</CardTitle>
            <CardDescription>Manage your schedule</CardDescription>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-gray-600 mb-4">Create slots, manage appointments, and track your bookings</p>
            <Link href="/register">
              <Button variant="outline" className="w-full bg-transparent">
                Join as Provider
              </Button>
            </Link>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>For Admins</CardTitle>
            <CardDescription>System analytics</CardDescription>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-gray-600 mb-4">Monitor system performance and user analytics</p>
            <Link href="/signin">
              <Button variant="secondary" className="w-full mt-5">
                Admin Access
              </Button>
            </Link>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
