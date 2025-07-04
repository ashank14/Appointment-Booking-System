"use client"

import { useState, useEffect } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu"
import { User, LogOut, Menu } from "lucide-react"
  import { usePathname } from "next/navigation"

export default function Navigation() {
  const [username, setUsername] = useState<any>(null)
  const [role,setRole]=useState<any>(null)
  const [isMenuOpen, setIsMenuOpen] = useState(false)
  const router = useRouter()

  const pathname = usePathname()

  useEffect(() => {
    const token = localStorage.getItem("token")
    const username = localStorage.getItem("username")
    const role= localStorage.getItem("role")
    if (token && role && username) {
      setRole(role)
      setUsername(username)
    }
  }, [pathname])

  const handleLogout = () => {
    localStorage.removeItem("token")
    localStorage.removeItem("username")
    setUsername(null)
    setRole(null)
    router.push("/")
  }

  const getDashboardLink = () => {
    if (!username) return "/"
    switch (role) {
      case "USER":
        return "/user/dashboard"
      case "PROVIDER":
        return "/provider/dashboard"
      case "ADMIN":
        return "/admin/dashboard"
      default:
        return "/"
    }
  }

  return (
    <nav className="bg-card text-card-foreground shadow-sm border-b border-border">
      <div className="container mx-auto px-4">
        <div className="flex justify-between items-center h-16">
          <Link href="/" className="text-xl font-bold text-foreground">
            BookingSmart
          </Link>

          <div className="hidden md:flex items-center space-x-4">
            {role ? (
              <>
                <Link href={getDashboardLink()}>
                  <Button variant="ghost">Dashboard</Button>
                </Link>
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button variant="ghost" size="sm">
                      <User className="h-4 w-4 mr-2" />
                      {username}
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    <DropdownMenuItem onClick={handleLogout}>
                      <LogOut className="h-4 w-4 mr-2" />
                      Logout
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </>
            ) : (
              <>
                <Link href="/signin">
                  <Button variant="ghost">Sign In</Button>
                </Link>
                <Link href="/register">
                  <Button>Register</Button>
                </Link>
              </>
            )}
          </div>

          <div className="md:hidden">
            <Button variant="ghost" size="sm" onClick={() => setIsMenuOpen(!isMenuOpen)}>
              <Menu className="h-4 w-4" />
            </Button>
          </div>
        </div>

        {isMenuOpen && (
          <div className="md:hidden py-4 border-t">
            {username ? (
              <>
                <Link href={getDashboardLink()} className="block py-2">
                  Dashboard
                </Link>
                <button onClick={handleLogout} className="block py-2 text-red-600">
                  Logout
                </button>
              </>
            ) : (
              <>
                <Link href="/signin" className="block py-2">
                  Sign In
                </Link>
                <Link href="/register" className="block py-2">
                  Register
                </Link>
              </>
            )}
          </div>
        )}
      </div>
    </nav>
  )
}
