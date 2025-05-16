import { CardTitle } from "@/components/ui/card";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { useAuth } from "@/contexts/AuthContext";
import { StyledCard, StyledCardHeader, StyledCardContent } from "@/components/ui/styled-card";
import { User } from "lucide-react";
import { Badge } from "@/components/ui/badge";

const UserProfileCard = () => {
  const { user } = useAuth();

  if (!user) return null;

  const getInitials = (firstName: string, lastName: string) => {
    return `${firstName[0]}${lastName[0]}`.toUpperCase();
  };

  const fullName = `${user.firstName} ${user.lastName}`;

  return (
    <StyledCard variant="primary">
      <StyledCardHeader>
        <div className="flex items-center space-x-2">
          <User className="h-5 w-5 text-blue-500" />
          <CardTitle className="text-lg font-medium">User Profile</CardTitle>
        </div>
        <p className="text-sm text-muted-foreground">Your account information</p>
      </StyledCardHeader>
      <StyledCardContent className="flex flex-col items-center justify-center py-6">
        <Avatar className="h-24 w-24 mb-4 bg-blue-100 border-2 border-blue-200">
          <AvatarFallback className="text-lg text-blue-700 bg-blue-100">{getInitials(user.firstName, user.lastName)}</AvatarFallback>
        </Avatar>
        <h3 className="text-xl font-bold text-gray-800">{fullName}</h3>
        <p className="text-sm text-muted-foreground mt-1">{user.email}</p>
        <div className="mt-4 flex flex-wrap gap-2 justify-center">
          {user.roles.map((role, index) => (
            <Badge key={index} variant="secondary" className="bg-blue-100 hover:bg-blue-200 text-blue-800 border-none">
              {role}
            </Badge>
          ))}
        </div>
      </StyledCardContent>
    </StyledCard>
  );
};

export default UserProfileCard;
